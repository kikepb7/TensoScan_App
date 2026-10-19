package com.tensoscan.ui.feature.scan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tensoscan.presentation.scan.ScanAnnouncement
import com.tensoscan.presentation.scan.ScanGuidance
import com.tensoscan.ui.R

@Composable
internal fun guidanceText(guidance: ScanGuidance): String = stringResource(
    when (guidance) {
        ScanGuidance.ALIGN_DISPLAY -> R.string.scan_guidance_align_display
        ScanGuidance.HOLD_STEADY -> R.string.scan_guidance_hold_steady
        ScanGuidance.POINT_AT_DISPLAY -> R.string.scan_guidance_point_at_display
        ScanGuidance.MOVE_CLOSER -> R.string.scan_guidance_move_closer
        ScanGuidance.LOW_LIGHT -> R.string.scan_guidance_low_light
        ScanGuidance.GLARE -> R.string.scan_guidance_glare
        ScanGuidance.DEVICE_MEASURING -> R.string.scan_guidance_device_measuring
        ScanGuidance.DEVICE_SHOWS_ERROR -> R.string.scan_guidance_device_error
        ScanGuidance.TIMED_OUT -> R.string.scan_guidance_timed_out
    },
)

/**
 * Shows the current advice. Its live region speaks only the rate-limited [announcement], so a screen reader is not
 * flooded every time the guidance flickers between two states (REQ-A002-44).
 */
@Composable
internal fun ScanGuidanceBanner(
    guidance: ScanGuidance,
    announcement: ScanAnnouncement?,
    modifier: Modifier = Modifier,
) {
    val spoken = announcement?.let { guidanceText(it.guidance) }.orEmpty()
    Text(
        text = guidanceText(guidance),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription = spoken
                liveRegion = LiveRegionMode.Polite
            },
    )
}
