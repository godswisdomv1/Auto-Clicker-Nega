package com.zdmgold.autoclickernega.core.model

/** Drives the one-time rating prompt: asked after 3 successful runs, and only once. */
data class RatingState(
    val successfulRuns: Int = 0,
    val prompted: Boolean = false,
)
