package com.zdmgold.autoclickernega.core.ocr

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.PackCause
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.port.TextRecognizer
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.Frame

/**
 * A recogniser for tests. It returns the blocks configured for a language that fall inside the requested region,
 * counts calls, and can pretend a language pack is missing.
 */
class FakeTextRecognizer(
    private val blocks: Map<String, List<TextBlock>> = emptyMap(),
    private val missingPacks: Set<String> = emptySet(),
) : TextRecognizer {
    var calls = 0
        private set

    override suspend fun read(
        frame: Frame,
        region: Rect,
        lang: String,
    ): Outcome<List<TextBlock>> {
        calls++
        if (lang in missingPacks) return Outcome.Failure(AppError.LanguagePack(PackCause.Missing))
        val inside =
            blocks[lang].orEmpty().filter { b ->
                b.box.l >= region.l && b.box.t >= region.t && b.box.r <= region.r && b.box.b <= region.b
            }
        return Outcome.Success(inside)
    }
}
