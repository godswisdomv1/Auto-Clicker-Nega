package com.zdmgold.autoclickernega.core.model

enum class ConsentDecision { Unknown, Permitted, Declined }

/** Whether any ad of any format may be requested. Unknown and Declined both mean no. */
data class ConsentState(
    val decision: ConsentDecision = ConsentDecision.Unknown,
    val privacyOptionsRequired: Boolean = false,
) {
    val canRequestAds: Boolean get() = decision == ConsentDecision.Permitted
}
