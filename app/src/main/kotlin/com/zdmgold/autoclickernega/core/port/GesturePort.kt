package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.vision.GestureOutcome

/** Performs one gesture on the screen and suspends until it has completed, been cancelled or been refused. */
interface GesturePort {
    suspend fun dispatch(g: Action.Gesture): GestureOutcome
}
