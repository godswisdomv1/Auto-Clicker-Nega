package com.zdmgold.autoclickernega.service.overlay

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.vision.MissReason
import java.util.Locale

/** What the HUD shows: the live match score, and why the last search did not lead to a tap. */
class HudModel(
    val score: Float?,
    val miss: MissReason,
)

/** The detection strip in the floating panel. It makes a silent miss visible instead of leaving the user guessing. */
class HudView(
    context: Context,
    theme: OverlayTheme,
) : TextView(context) {
    init {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        setTextColor(theme.textSecondary)
        typeface = theme.regular
        gravity = Gravity.CENTER
        maxWidth = theme.dp(MAX_WIDTH_DP)
        visibility = View.GONE
    }

    fun show(model: HudModel?) {
        val lines =
            listOfNotNull(
                model?.score?.let {
                    context.getString(
                        R.string.hud_score,
                        String.format(Locale.getDefault(), "%.2f", it),
                    )
                },
                model?.let { reasonText(it.miss) },
            )
        text = lines.joinToString("\n")
        visibility = if (lines.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun reasonText(miss: MissReason): String? {
        val res =
            when (miss) {
                MissReason.None -> return null
                MissReason.BelowConfidence -> R.string.hud_below_confidence
                MissReason.NotStable -> R.string.hud_not_stable
                MissReason.RegionTooSmall -> R.string.hud_region_too_small
                MissReason.TemplateTooFlat -> R.string.hud_template_flat
                MissReason.TemplateMissing -> R.string.hud_template_missing
                MissReason.CaptureUnavailable -> R.string.hud_cant_see
                MissReason.LanguagePackMissing -> R.string.hud_language_pack
                MissReason.NoColorData -> R.string.hud_no_color
                MissReason.NoNumber -> R.string.hud_no_number
            }
        return context.getString(res)
    }

    private companion object {
        const val TEXT_SP = 12f
        const val MAX_WIDTH_DP = 180f
    }
}
