package com.zdmgold.autoclickernega.service.bridge

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.EngineEvent
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ServiceBridge
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The in-process link between the app and the accessibility service. The service registers its engine when it
 * connects and unregisters when it unbinds. With no service bound the state is ServiceLost, and every command
 * returns a typed error instead of failing silently. The UI uses this and nothing else from the service layer.
 */
object EngineBridge : ServiceBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val _state = MutableStateFlow<EngineState>(EngineState.ServiceLost)
    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = EVENT_BUFFER)
    private val _counters = MutableStateFlow<Map<String, Int>>(emptyMap())
    private var host: ServiceBridge? = null
    private var jobs = emptyList<Job>()

    override val state: StateFlow<EngineState> = _state.asStateFlow()
    override val events: SharedFlow<EngineEvent> = _events.asSharedFlow()
    override val counters: StateFlow<Map<String, Int>> = _counters.asStateFlow()

    /** Called by the service on connect. A previous host is replaced. */
    @Synchronized
    fun register(engine: ServiceBridge) {
        unregister()
        host = engine
        jobs =
            listOf(
                scope.launch { engine.state.collect { _state.value = it } },
                scope.launch { engine.events.collect { _events.emit(it) } },
                scope.launch { engine.counters.collect { _counters.value = it } },
            )
    }

    /** Called by the service on unbind. */
    @Synchronized
    fun unregister() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        host = null
        _state.value = EngineState.ServiceLost
        _counters.value = emptyMap()
    }

    override fun start(request: RunRequest): Outcome<Unit> = withHost { it.start(request) }

    override fun pause(): Outcome<Unit> = withHost { it.pause() }

    override fun resume(): Outcome<Unit> = withHost { it.resume() }

    override fun stop(): Outcome<Unit> = withHost { it.stop() }

    @Synchronized
    private fun withHost(command: (ServiceBridge) -> Outcome<Unit>): Outcome<Unit> =
        host?.let(command) ?: Outcome.Failure(AppError.ServiceNotEnabled)

    private const val EVENT_BUFFER = 64
}
