package com.zdmgold.autoclickernega.core.model

/** Why a run ended. */
enum class StopReason { User, Timer, Done, Cap, ScreenOff, ServiceLost, Blocked, WaitCancelled }

enum class EngineBlockReason { GesturesRejected, CaptureUnavailable }

/** What the engine is doing right now, as seen by the panel, the HUD and Home. */
sealed interface EngineState {
    data object Idle : EngineState

    /** The Start in X wait. */
    data class Waiting(
        val scriptId: String,
        val remainingMs: Long,
        val totalMs: Long,
    ) : EngineState

    /** The short countdown just before a run. */
    data class Countdown(
        val scriptId: String,
        val secondsLeft: Int,
    ) : EngineState

    data class Running(
        val scriptId: String,
        val startedAtMs: Long,
        val capMs: Long?,
    ) : EngineState

    data class Paused(
        val scriptId: String,
    ) : EngineState

    data class Blocked(
        val reason: EngineBlockReason,
    ) : EngineState

    data object ServiceLost : EngineState
}

/** One-off things that happen during a run. */
sealed interface EngineEvent {
    data class Stopped(
        val scriptId: String,
        val reason: StopReason,
    ) : EngineEvent

    data class EventFired(
        val scriptId: String,
        val eventId: String,
    ) : EngineEvent

    data class GestureRejected(
        val consecutive: Int,
    ) : EngineEvent
}
