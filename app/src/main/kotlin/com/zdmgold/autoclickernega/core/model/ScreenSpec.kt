package com.zdmgold.autoclickernega.core.model

/** The screen a script was authored on, used to rescale it on a different screen. */
data class ScreenSpec(
    val w: Int,
    val h: Int,
    val density: Int,
) {
    val isLandscape: Boolean get() = w > h
}
