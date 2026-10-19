package com.tensoscan.ui.feature.scan.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.tensoscan.presentation.scan.BoxRole
import com.tensoscan.presentation.scan.DetectionBoxUiState
import com.tensoscan.ui.feature.scan.RoiGeometry

private val GuideCornerRadius = 16.dp
private val GuideStrokeWidth = 4.dp
private val BoxStrokeWidth = 2.dp
private const val SCRIM_ALPHA = 0.55f
private const val IDLE_BORDER_ALPHA = 0.7f

/** Colour used for the glyphs of each number; the live panel uses the same ones so the user can match them. */
@Composable
internal fun roleColor(role: BoxRole): Color = when (role) {
    BoxRole.SYSTOLIC -> MaterialTheme.colorScheme.primary
    BoxRole.DIASTOLIC -> MaterialTheme.colorScheme.tertiary
    BoxRole.PULSE -> MaterialTheme.colorScheme.secondary
    BoxRole.OTHER -> Color.White.copy(alpha = IDLE_BORDER_ALPHA)
}

/**
 * Dims everything outside the guide rectangle, draws the border (which fills with colour as the reading stabilises) and
 * the boxes of the detected glyphs. It uses the same geometry as the crop that is analysed, so what the user sees
 * inside the frame is what is read.
 */
@Composable
internal fun GuideOverlay(
    boxes: List<DetectionBoxUiState>,
    progress: Float,
    imageAspect: Float,
    modifier: Modifier = Modifier,
) {
    val idleColor = Color.White.copy(alpha = IDLE_BORDER_ALPHA)
    val activeColor = MaterialTheme.colorScheme.primary
    val roleColors = BoxRole.entries.associateWith { roleColor(it) }

    Canvas(modifier = modifier) {
        val guide = RoiGeometry.toView(
            roi = RoiGeometry.guideRectForAspect(imageAspect),
            viewWidth = size.width,
            viewHeight = size.height,
            imageAspect = imageAspect,
        )
        val topLeft = Offset(guide.left, guide.top)
        val guideSize = Size(guide.width, guide.height)
        val corner = CornerRadius(GuideCornerRadius.toPx())

        val scrim = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
            addRoundRect(RoundRect(androidx.compose.ui.geometry.Rect(topLeft, guideSize), corner))
        }
        drawPath(scrim, Color.Black.copy(alpha = SCRIM_ALPHA))

        drawRoundRect(
            color = lerp(idleColor, activeColor, progress.coerceIn(0f, 1f)),
            topLeft = topLeft,
            size = guideSize,
            cornerRadius = corner,
            style = Stroke(width = GuideStrokeWidth.toPx()),
        )

        boxes.forEach { box ->
            drawRect(
                color = roleColors.getValue(box.role),
                topLeft = Offset(guide.left + box.left * guide.width, guide.top + box.top * guide.height),
                size = Size((box.right - box.left) * guide.width, (box.bottom - box.top) * guide.height),
                style = Stroke(width = BoxStrokeWidth.toPx()),
            )
        }
    }
}
