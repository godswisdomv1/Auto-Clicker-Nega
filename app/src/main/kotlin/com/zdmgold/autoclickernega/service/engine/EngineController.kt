package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.EngineEvent
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ServiceBridge
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Runs one script at a time inside the accessibility service and exposes it through the bridge contract. */
class EngineController(
    private val deps: RunDeps,
    private val scope: CoroutineScope,
) : ServiceBridge {
    private val sinks = EngineSinks()
    private var session: RunSession? = null

    override val state: StateFlow<EngineState> = sinks.state.asStateFlow()
    override val events: SharedFlow<EngineEvent> = sinks.events.asSharedFlow()
    override val counters: StateFlow<Map<String, Int>> = sinks.counters.asStateFlow()

    override fun start(request: RunRequest): Outcome<Unit> {
        if (session?.isActive == true) return Outcome.Failure(AppError.Unexpected("a run is already active"))
        sinks.counters.value = emptyMap()
        RunSession(deps, request, scope, sinks).also {
            session = it
            it.start()
        }
        return Outcome.Success(Unit)
    }

    override fun pause(): Outcome<Unit> = command { it.pause() }

    override fun resume(): Outcome<Unit> = command { it.resume() }

    override fun stop(): Outcome<Unit> = command { it.stop() }

    /** The screen turned off. Runs, and a Start in X wait, end here. */
    fun onScreenOff() {
        session?.screenOff()
    }

    /** The service is being unbound. */
    fun onServiceLost() {
        session?.serviceLost()
    }

    private fun command(action: (RunSession) -> Unit): Outcome<Unit> {
        session?.takeIf { it.isActive }?.let(action)
        return Outcome.Success(Unit)
    }
}
