package com.zdmgold.autoclickernega.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import com.zdmgold.autoclickernega.ui.theme.Motion
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * The press behaviour of every tappable thing: no ripple, no indication, only the content fading to 60 percent
 * while it is held. With reduced motion the change is instant.
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed && enabled) Tokens.PRESSED_ALPHA else 1f,
        animationSpec = tween(Motion.duration(Motion.PRESS_MS, NegaTheme.reduceMotion), easing = Motion.easeInOut),
        label = "press",
    )
    return alpha(alpha).clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onClick = onClick,
    )
}
