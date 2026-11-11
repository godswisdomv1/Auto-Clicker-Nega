package com.zdmgold.autoclickernega.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens
import kotlin.math.hypot

/**
 * Picks a search region on a small copy of the script's screen. Drag to draw a rectangle, drag a corner to resize
 * it, or drag inside it to move it. With no region the whole screen is searched.
 */
@Composable
fun RegionPicker(
    screen: ScreenSpec,
    region: Rect?,
    onRegionChange: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NegaTheme.colors
    val shape = RoundedCornerShape(Tokens.Radius.chip)
    val current by rememberUpdatedState(region)
    val report by rememberUpdatedState(onRegionChange)
    var widthPx by remember { mutableStateOf(0) }
    var kind by remember { mutableStateOf(RegionMath.NONE) }
    var origin by remember { mutableStateOf<Rect?>(null) }
    var start by remember { mutableStateOf(Offset.Zero) }
    val ratio = screen.w.toFloat() / screen.h.toFloat()
    val description = stringResource(R.string.region_description)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth(if (ratio < 1f) PORTRAIT_WIDTH_FRACTION else 1f)
                        .aspectRatio(ratio)
                        .clip(shape)
                        .background(colors.bgTertiary, shape)
                        .border(Tokens.Stroke.hairline, colors.borderSubtle, shape)
                        .onSizeChanged { widthPx = it.width }
                        .semantics { contentDescription = description }
                        .pointerInput(screen) {
                            detectDragGestures(
                                onDragStart = { at ->
                                    val scale = screen.w.toFloat() / widthPx.coerceAtLeast(1)
                                    val sx = at.x * scale
                                    val sy = at.y * scale
                                    start = Offset(sx, sy)
                                    val r = current
                                    val corner =
                                        if (r !=
                                            null
                                        ) {
                                            RegionMath.hitCorner(r, sx, sy, scale * HIT_PX)
                                        } else {
                                            RegionMath.NONE
                                        }
                                    origin = r
                                    kind =
                                        when {
                                            r != null && corner >= 0 -> corner
                                            r != null && RegionMath.contains(r, sx, sy) -> RegionMath.MOVE
                                            else -> RegionMath.NONE
                                        }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val scale = screen.w.toFloat() / widthPx.coerceAtLeast(1)
                                    val x = (change.position.x * scale).toInt()
                                    val y = (change.position.y * scale).toInt()
                                    val from = origin
                                    report(
                                        when {
                                            kind == RegionMath.MOVE && from != null -> {
                                                RegionMath.shifted(
                                                    from,
                                                    x - start.x.toInt(),
                                                    y - start.y.toInt(),
                                                    screen,
                                                )
                                            }

                                            kind >= 0 && from != null -> {
                                                RegionMath.resized(from, kind, x, y, screen)
                                            }

                                            else -> {
                                                RegionMath.drawn(start.x.toInt(), start.y.toInt(), x, y, screen)
                                            }
                                        },
                                    )
                                },
                                onDragEnd = {
                                    val r = current
                                    if (r != null && !RegionMath.isUsable(r)) report(null)
                                    kind = RegionMath.NONE
                                },
                                onDragCancel = { kind = RegionMath.NONE },
                            )
                        },
            ) {
                val r = region ?: return@Canvas
                val scale = size.width / screen.w
                val topLeft = Offset(r.l * scale, r.t * scale)
                val box = Size(r.width * scale, r.height * scale)
                drawRect(colors.accent.copy(alpha = FILL_ALPHA), topLeft, box)
                drawRect(colors.accent, topLeft, box, style = Stroke(width = Tokens.Stroke.icon.toPx()))
                RegionMath.corners(r).forEach {
                    drawCircle(colors.accent, Metrics.regionHandle.toPx() / 2, Offset(it.x * scale, it.y * scale))
                }
            }
        }
        if (region != null) {
            SecondaryButton(stringResource(R.string.region_clear), { onRegionChange(null) }, Modifier.fillMaxWidth())
        } else {
            NegaText(
                stringResource(R.string.region_full_screen),
                style = NegaTheme.typography.footnote,
                color = colors.textSecondary,
            )
        }
    }
}

private const val PORTRAIT_WIDTH_FRACTION = 0.45f
private const val HIT_PX = 28f
private const val FILL_ALPHA = 0.18f
