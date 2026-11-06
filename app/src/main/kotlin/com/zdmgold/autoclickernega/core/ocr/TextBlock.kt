package com.zdmgold.autoclickernega.core.ocr

import com.zdmgold.autoclickernega.core.model.Rect

/** A piece of recognised text, with where it is on the screen and how sure the engine was (0 to 1). */
data class TextBlock(
    val text: String,
    val box: Rect,
    val confidence: Float,
)
