package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.ocr.TextBlock
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.Frame

/**
 * Reads text from part of a frame. The engine behind it is chosen by the OCR bake-off and hidden here.
 * A missing language pack is a typed failure, never an empty result, so it is not mistaken for "no text".
 */
interface TextRecognizer {
    suspend fun read(
        frame: Frame,
        region: Rect,
        lang: String,
    ): Outcome<List<TextBlock>>
}
