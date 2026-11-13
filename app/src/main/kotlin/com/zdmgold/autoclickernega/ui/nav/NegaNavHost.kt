package com.zdmgold.autoclickernega.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.zdmgold.autoclickernega.ui.theme.Motion
import com.zdmgold.autoclickernega.ui.theme.NegaTheme

/**
 * Shows the screen on top of the back stack. A screen coming in fades and slides 20 px over 350 ms and one going
 * out takes 280 ms, with no spring or bounce. With reduced motion the change is instant. The system back gesture
 * pops the stack, with the predictive animation the platform provides.
 */
@Composable
fun NegaNavHost(
    stack: BackStack,
    onBack: () -> Unit,
    content: @Composable (Route) -> Unit,
) {
    val reduced = NegaTheme.reduceMotion
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val side = if (rtl) -1 else 1
    BackHandler(enabled = stack.canPop, onBack = onBack)
    val current = stack.current ?: return
    AnimatedContent(
        targetState = current,
        transitionSpec = { transition(stack.direction, side, reduced) },
        label = "screens",
    ) { route -> content(route) }
}

private fun transition(
    direction: NavDirection,
    side: Int,
    reduced: Boolean,
): ContentTransform {
    val popping = direction == NavDirection.Pop
    val ms = Motion.duration(if (popping) Motion.POP_MS else Motion.PUSH_MS, reduced)
    val enterFrom = if (popping) -Motion.SLIDE_PX else Motion.SLIDE_PX
    val exitTo = -enterFrom
    val enter =
        fadeIn(tween(ms, easing = Motion.easeInOut)) +
            slideInHorizontally(tween(ms, easing = Motion.easeInOut)) { enterFrom * side }
    val exit =
        fadeOut(tween(ms, easing = Motion.easeInOut)) +
            slideOutHorizontally(tween(ms, easing = Motion.easeInOut)) { exitTo * side }
    return if (direction == NavDirection.Reset) {
        fadeIn(tween(ms, easing = Motion.easeInOut)) togetherWith fadeOut(tween(ms, easing = Motion.easeInOut))
    } else {
        enter togetherWith exit
    }
}
