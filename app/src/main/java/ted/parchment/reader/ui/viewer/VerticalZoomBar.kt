package ted.parchment.reader.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

/**
 * Pill-shaped vertical zoom control for the right edge of the reading area.
 *
 * Layout:
 * ```
 *  ┌────┐
 *  │ +  │  ← tap to zoom in  (+0.25×)
 *  │    │
 *  │ ●  │  ← draggable thumb (top = 5×, bottom = 1×)
 *  │    │
 *  │ −  │  ← tap to zoom out (−0.25×)
 *  │100%│  ← live zoom percentage
 *  └────┘
 * ```
 */
@Composable
fun VerticalZoomBar(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
    val minZoom = 1f
    val maxZoom = 5f
    val step = 0.25f
    val density = LocalDensity.current

    val currentScale by rememberUpdatedState(scale)
    val currentOnScaleChange by rememberUpdatedState(onScaleChange)

    var trackHeightPx by remember { mutableIntStateOf(1) }

    val fraction = ((scale - minZoom) / (maxZoom - minZoom)).coerceIn(0f, 1f)

    // Safe margin so the thumb stays away from the +/- buttons
    val verticalMarginDp = 12.dp
    val verticalMarginPx = with(density) { verticalMarginDp.toPx() }

    Column(
        modifier = Modifier
            .shadow(10.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(surfaceColor.copy(alpha = 0.94f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Zoom-in button ──────────────────────────────────────────────────
        ZoomButton(
            label = "+",
            enabled = scale < maxZoom,
            accentColor = accentColor,
            textColor = textColor,
            onClick = { onScaleChange((scale + step).coerceAtMost(maxZoom)) }
        )

        // ── Draggable track ─────────────────────────────────────────────────
        val trackColor = textColor.copy(alpha = 0.12f)
        val filledColor = accentColor.copy(alpha = 0.35f)
        val thumbColor = accentColor
        val thumbGlow = accentColor.copy(alpha = 0.20f)

        Canvas(
            modifier = Modifier
                .width(24.dp)
                .height(180.dp)
                .onSizeChanged { trackHeightPx = it.height }
                .pointerInput(trackHeightPx, minZoom, maxZoom, verticalMarginPx) {
                    awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            var didDrag = false
                            var prevY = down.position.y
                            val activeTrackPx = (trackHeightPx - 2 * verticalMarginPx).coerceAtLeast(1f)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break

                                val dy = change.position.y - prevY
                                if (kotlin.math.abs(dy) > 0f && activeTrackPx > 0) {
                                    change.consume()
                                    didDrag = true
                                    val delta = -(dy / activeTrackPx) * (maxZoom - minZoom)
                                    currentOnScaleChange((currentScale + delta).coerceIn(minZoom, maxZoom))
                                }
                                prevY = change.position.y
                            }

                            if (!didDrag && activeTrackPx > 0) {
                                val tapY = (down.position.y - verticalMarginPx).coerceIn(0f, activeTrackPx)
                                val newFraction = 1f - (tapY / activeTrackPx)
                                currentOnScaleChange(
                                    (minZoom + newFraction * (maxZoom - minZoom)).coerceIn(minZoom, maxZoom)
                                )
                            }
                    }
                }
        ) {
            val cx = size.width / 2f
            val totalH = size.height
            val activeH = totalH - 2 * verticalMarginPx
            val trackW = with(density) { 4.dp.toPx() }
            val thumbR = with(density) { 6.dp.toPx() }
            val glowR = with(density) { 10.dp.toPx() }
            val halfTrack = trackW / 2f
            val thumbY = verticalMarginPx + activeH * (1f - fraction)

            // Track background
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(cx - halfTrack, verticalMarginPx),
                size = Size(trackW, activeH),
                cornerRadius = CornerRadius(halfTrack)
            )

            // Filled segment
            if (thumbY < totalH - verticalMarginPx) {
                drawRoundRect(
                    color = filledColor,
                    topLeft = Offset(cx - halfTrack, thumbY),
                    size = Size(trackW, (totalH - verticalMarginPx) - thumbY),
                    cornerRadius = CornerRadius(halfTrack)
                )
            }

            // Thumb with glow
            drawCircle(color = thumbGlow, radius = glowR, center = Offset(cx, thumbY))
            drawCircle(color = thumbColor, radius = thumbR, center = Offset(cx, thumbY))
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = thumbR * 0.48f,
                center = Offset(cx - thumbR * 0.18f, thumbY - thumbR * 0.22f)
            )
        }

        // ── Zoom-out button ─────────────────────────────────────────────────
        ZoomButton(
            label = "−",
            enabled = scale > minZoom,
            accentColor = accentColor,
            textColor = textColor,
            onClick = { onScaleChange((scale - step).coerceAtLeast(minZoom)) }
        )

        // ── Live zoom label ─────────────────────────────────────────────────
        Text(
            text = "${(scale * 100).toInt()}%",
            fontSize = 10.sp,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Small square button for zoom increment/decrement.
 */
@Composable
private fun ZoomButton(
    label: String,
    enabled: Boolean,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = if (enabled) 0.15f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) accentColor else textColor.copy(alpha = 0.25f),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 18.sp
        )
    }
}
