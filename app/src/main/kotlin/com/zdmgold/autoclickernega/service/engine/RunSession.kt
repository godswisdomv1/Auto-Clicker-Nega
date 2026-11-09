package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.CaptureCause
import com.zdmgold.autoclickernega.core.error.WaitCancelCause
import com.zdmgold.autoclickernega.core.model.EngineBlockReason
import com.zdmgold.autoclickernega.core.model.EngineEvent
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.StopReason
import com.zdmgold.autoclickernega.core.port.Clock
import com.zdmgold.autoclickernega.core.port.FrameSource
import com.zdmgold.autoclickernega.core.port.GesturePort
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ScriptResolver
import com.zdmgold.autoclickernega.core.port.TemplateProvider
import com.zdmgold.autoclickernega.core.port.TextRecognizer
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.run.CadenceController
import com.zdmgold.autoclickernega.core.run.ConditionEvaluator
import com.zdmgold.autoclickernega.core.run.CounterStore
import com.zdmgold.autoclickernega.core.run.EventScheduler
import com.zdmgold.autoclickernega.core.run.ExecSignal
import com.zdmgold.autoclickernega.core.run.LoopGuard
import com.zdmgold.autoclickernega.core.run.RunContext
import com.zdmgold.autoclickernega.core.script.CoordinateScaler
import com.zdmgold.autoclickernega.core.script.EventCompiler
import com.zdmgold.autoclickernega.core.script.usesColor
import com.zdmgold.autoclickernega.core.vision.DetectionEntry
import com.zdmgold.autoclickernega.core.vision.DetectionLog
import com.zdmgold.autoclickernega.core.vision.Frame
import com.zdmgold.autoclickernega.core.vision.GestureOutcome
import com.zdmgold.autoclickernega.core.vision.MissReason
import com.zdmgold.autoclickernega.core.vision.TemplateScaler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

/** Everything a run needs from the outside world. */
class RunDeps(
    val clock: Clock,
    val gestures: GesturePort,
    val frames: (needsColor: Boolean) -> FrameSource,
    val recognizer: TextRecognizer?,
    val templates: TemplateProvider,
    val resolver: ScriptResolver,
    val panelMask: () -> Rect? = { null },
    val log: DetectionLog = DetectionLog(),
    val osMinIntervalMs: Long = CadenceController.DEFAULT_OS_MIN_INTERVAL_MS,
)

/** The outputs of a run, shared with whoever watches it. */
class EngineSinks {
    val state = MutableStateFlow<EngineState>(EngineState.Idle)
    val events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = EVENT_BUFFER)
    val counters = MutableStateFlow<Map<String, Int>>(emptyMap())

    private companion object {
        const val EVENT_BUFFER = 64
    }
}

/**
 * One run, from the Start in X wait through the countdown to the end. Time spent waiting is part of the run that
 * was gated when Start was pressed. A run ends for exactly one reason, and that reason is always reported.
 */
class RunSession(
    private val deps: RunDeps,
    private val request: RunRequest,
    private val scope: CoroutineScope,
    private val sinks: EngineSinks = EngineSinks(),
) {
    private class StopRequest(
        val reason: StopReason,
        val waitCause: WaitCancelCause? = null,
    )

    val state: StateFlow<EngineState> = sinks.state.asStateFlow()
    val events: SharedFlow<EngineEvent> = sinks.events.asSharedFlow()
    val counters = CounterStore(request.script.counters) { sinks.counters.value = it }

    private val scriptId = request.script.id
    private val paused = MutableStateFlow(false)
    private val guard = LoopGuard()
    private val executor =
        ActionExecutor(
            deps.gestures,
            counters,
            ::runChild,
            onRejected = { sinks.events.tryEmit(EngineEvent.GestureRejected(it)) },
        )
    private var stopRequest: StopRequest? = null
    private var job: Job? = null
    private var runStartMs = 0L

    val isActive: Boolean get() = job?.isActive == true

    fun start(): Job {
        check(job == null) { "a session runs once" }
        sinks.counters.value = counters.snapshot()
        return scope.launch { execute() }.also { job = it }
    }

    fun pause() {
        if (sinks.state.value is EngineState.Running) {
            paused.value = true
            sinks.state.value = EngineState.Paused(scriptId)
        }
    }

    fun resume() {
        if (sinks.state.value is EngineState.Paused) {
            sinks.state.value = EngineState.Running(scriptId, runStartMs, request.capMs)
            paused.value = false
        }
    }

    /** The user pressed Stop, or Cancel during the Start in X wait. */
    fun stop() = requestStop(StopReason.User, WaitCancelCause.User)

    fun screenOff() = requestStop(StopReason.ScreenOff, WaitCancelCause.ScreenOff)

    fun serviceLost() = requestStop(StopReason.ServiceLost, WaitCancelCause.ServiceLost)

    private fun requestStop(
        reason: StopReason,
        cause: WaitCancelCause,
    ) {
        if (stopRequest != null) return
        stopRequest =
            if (sinks.state.value is EngineState.Waiting) {
                StopRequest(StopReason.WaitCancelled, cause)
            } else {
                StopRequest(reason)
            }
        job?.cancel()
    }

    private suspend fun execute() {
        val end =
            try {
                runPhases()
            } catch (e: CancellationException) {
                stopRequest ?: StopRequest(StopReason.User).also { e.message }
            }
        finish(end)
    }

    private fun finish(end: StopRequest) {
        val lost = end.reason == StopReason.ServiceLost || end.waitCause == WaitCancelCause.ServiceLost
        sinks.state.value =
            when {
                end.reason == StopReason.Blocked -> EngineState.Blocked(EngineBlockReason.GesturesRejected)
                lost -> EngineState.ServiceLost
                else -> EngineState.Idle
            }
        val cause = end.waitCause.takeIf { end.reason == StopReason.WaitCancelled }
        sinks.events.tryEmit(EngineEvent.Stopped(scriptId, end.reason, cause))
    }

    private suspend fun runPhases(): StopRequest {
        waiting(request.script.run.startDelayMs)
        countdown(request.script.run.countdownSec)
        return coroutineScope {
            runStartMs = deps.clock.nowMs()
            val limit = effectiveLimit()
            sinks.state.value = EngineState.Running(scriptId, runStartMs, request.capMs)
            val watchdog =
                limit?.let {
                    launch {
                        deps.clock.delayUntil(runStartMs + it.ms)
                        requestStop(it.reason, WaitCancelCause.User)
                    }
                }
            guard.enter(scriptId)
            val signal = runBody(request.script, child = false)
            watchdog?.cancel()
            StopRequest(if (signal == ExecSignal.Blocked) StopReason.Blocked else StopReason.Done)
        }
    }

    private class Limit(
        val ms: Long,
        val reason: StopReason,
    )

    private fun effectiveLimit(): Limit? {
        val timer =
            request.script.run.globalTimerMs
                .takeIf { it > 0 }
                ?.let { Limit(it, StopReason.Timer) }
        val cap = request.capMs?.let { Limit(it, StopReason.Cap) }
        return listOfNotNull(timer, cap).minByOrNull { it.ms }
    }

    private suspend fun waiting(total: Long) {
        if (total <= 0) return
        val end = deps.clock.nowMs() + total
        while (true) {
            val now = deps.clock.nowMs()
            val remaining = end - now
            if (remaining <= 0) break
            sinks.state.value = EngineState.Waiting(scriptId, remaining, total)
            deps.clock.delayUntil(now + minOf(remaining, TICK_MS))
        }
    }

    private suspend fun countdown(seconds: Int) {
        for (left in seconds downTo 1) {
            sinks.state.value = EngineState.Countdown(scriptId, left)
            deps.clock.delayUntil(deps.clock.nowMs() + TICK_MS)
        }
    }

    private suspend fun awaitResumed() {
        if (paused.value) paused.first { !it }
    }

    private suspend fun runChild(id: String): ExecSignal {
        val child = deps.resolver.resolve(id) ?: return ExecSignal.Continue
        if (!guard.enter(id)) return ExecSignal.Continue
        try {
            counters.register(child.counters)
            val signal = runBody(child, child = true)
            return if (signal == ExecSignal.Stop) ExecSignal.Continue else signal
        } finally {
            guard.exit(id)
        }
    }

    private suspend fun runBody(
        script: Script,
        child: Boolean,
    ): ExecSignal =
        when (script.body) {
            is ScriptBody.Simple -> {
                SequenceRunner(deps.clock, executor, ::awaitResumed)
                    .run(script, request.screen, request.seed, if (child && script.run.loops == 0) 1 else null)
            }

            is ScriptBody.Smart -> {
                runSmart(script)
            }
        }

    private suspend fun loadTemplates(script: Script): Map<String, Frame> {
        val ratio = CoordinateScaler.densityRatio(script.screen, request.screen)
        val out = HashMap<String, Frame>()
        for (ref in script.templates.values) {
            deps.templates.load(script.id, ref)?.let { out[ref.id] = TemplateScaler.scale(it, ratio) }
        }
        return out
    }

    private suspend fun runSmart(script: Script): ExecSignal {
        val plan = (EventCompiler.compile(script) as? Outcome.Success)?.value ?: return ExecSignal.Stop
        val scheduler = EventScheduler(plan)
        val ctx = RunContext(deps.clock, counters, runStartMs, scheduler::lastFiredAt)
        val evaluator = ConditionEvaluator(loadTemplates(script), deps.recognizer)
        val pipeline =
            DetectionPipeline(
                deps.frames(script.usesColor()),
                deps.clock,
                CadenceController(script.run.searchRate, deps.osMinIntervalMs),
                deps.panelMask,
            )
        val runner =
            EventRunner(
                scheduler = scheduler,
                evaluator = evaluator,
                ctx = ctx,
                executor = executor,
                log = deps.log,
                clock = deps.clock,
                scriptScreen = script.screen,
                runningScreen = request.screen,
                seed = request.seed,
                onFired = { sinks.events.tryEmit(EngineEvent.EventFired(scriptId, it)) },
                onAction = pipeline::onAction,
            )
        var result = ExecSignal.Continue
        pipeline
            .frames()
            .takeWhile { event ->
                awaitResumed()
                result =
                    when (event) {
                        is FrameEvent.Captured -> {
                            ctx.frameIndex = event.index
                            runner.onFrame(event.frame, event.timeMs)
                        }

                        is FrameEvent.Unavailable -> {
                            unavailable(event.error)
                        }
                    }
                result == ExecSignal.Continue
            }.collect()
        return result
    }

    /** A frame could not be captured. The run keeps going and makes no tap for it. */
    private fun unavailable(error: AppError): ExecSignal {
        val cause = (error as? AppError.CaptureUnavailable)?.cause ?: CaptureCause.Internal
        sinks.events.tryEmit(EngineEvent.CaptureUnavailable(cause))
        deps.log.add(
            DetectionEntry(
                deps.clock.nowMs(),
                null,
                null,
                null,
                null,
                MissReason.CaptureUnavailable,
                GestureOutcome.None,
                0,
            ),
        )
        return ExecSignal.Continue
    }

    private companion object {
        const val TICK_MS = 1000L
    }
}
