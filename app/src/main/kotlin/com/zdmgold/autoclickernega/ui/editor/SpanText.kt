package com.zdmgold.autoclickernega.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zdmgold.autoclickernega.R

/** A duration as a person says it: "1 h 30 min", "45 s", "40 ms". */
@Composable
fun spanText(ms: Long): String =
    SpanParts.of(ms).joinToString(" ") { part ->
        val res =
            when (part.unit) {
                SpanUnit.Hours -> R.string.span_h
                SpanUnit.Minutes -> R.string.span_min
                SpanUnit.Seconds -> R.string.span_s
                SpanUnit.Millis -> R.string.span_ms
            }
        stringResource(res, part.value)
    }
