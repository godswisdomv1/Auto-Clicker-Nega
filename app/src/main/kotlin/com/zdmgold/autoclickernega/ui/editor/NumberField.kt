package com.zdmgold.autoclickernega.ui.editor

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.zdmgold.autoclickernega.ui.components.NegaTextField

private const val MAX_DIGITS = 9

/**
 * A whole-number entry. The text is kept apart from the value, so clearing the field does not force a number in.
 * [resetKey] makes the field start again from [value], for when it is reused for another step.
 */
@Composable
fun NumberField(
    label: String,
    value: Long,
    onValue: (Long) -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any = Unit,
) {
    var text by remember(resetKey) { mutableStateOf(value.toString()) }
    NegaTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(MAX_DIGITS)
            text = digits
            digits.toLongOrNull()?.let(onValue)
        },
        modifier = modifier,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
fun NumberField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any = Unit,
) = NumberField(label, value.toLong(), { onValue(it.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }, modifier, resetKey)
