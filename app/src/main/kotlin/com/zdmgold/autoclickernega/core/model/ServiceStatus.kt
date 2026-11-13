package com.zdmgold.autoclickernega.core.model

/** Whether the accessibility service can do its job right now. */
enum class ServiceStatus {
    /** Not enabled in the system accessibility settings. */
    Off,

    /** Enabled, but not connected. The system may have stopped it, and turning it off and on again fixes it. */
    Connecting,

    /** Enabled and connected. */
    Ready,
    ;

    companion object {
        fun of(
            enabledInSettings: Boolean,
            connected: Boolean,
        ): ServiceStatus =
            when {
                !enabledInSettings -> Off
                connected -> Ready
                else -> Connecting
            }
    }
}
