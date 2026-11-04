package com.zdmgold.autoclickernega.core.model

/** Whether the one-time Pro purchase is held, and when Play last confirmed it. */
data class Entitlement(
    val isPro: Boolean = false,
    val verifiedAtMs: Long? = null,
) {
    companion object {
        val Free = Entitlement()
    }
}
