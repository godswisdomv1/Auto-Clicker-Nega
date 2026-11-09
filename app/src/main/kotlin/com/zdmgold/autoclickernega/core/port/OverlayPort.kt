package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** A reference image cut from the live screen: where it was, and its PNG bytes. */
class CropResult(
    val box: Rect,
    val png: ByteArray,
)

/** What the app can ask of the floating overlay, which lives inside the accessibility service. */
interface OverlayPort {
    val panelVisible: StateFlow<Boolean>

    /** Crops the user took with the panel's own "add image target" button. The wizard and the editors listen here. */
    val panelCrops: SharedFlow<CropResult>

    fun showPanel(): Outcome<Unit>

    fun hidePanel(): Outcome<Unit>

    /**
     * Dims the screen and lets the user drag a rectangle over what to find. Returns the cut-out image, or null when
     * the user cancels. The picture is taken from the screen as it was before the dimming.
     */
    suspend fun captureCrop(): Outcome<CropResult?>
}
