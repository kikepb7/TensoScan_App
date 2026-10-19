package com.tensoscan.ui.feature.scan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tensoscan.presentation.scan.BoxRole
import com.tensoscan.presentation.scan.LiveValueUiState
import com.tensoscan.ui.R

private const val PROVISIONAL_ALPHA = 0.65f

/** The three values read so far; one that is still unknown shows "—". Stable values are drawn at full strength. */
@Composable
internal fun LiveReadingPanel(
    systolic: LiveValueUiState,
    diastolic: LiveValueUiState,
    pulse: LiveValueUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LiveValue(stringResource(R.string.scan_label_systolic), systolic, BoxRole.SYSTOLIC)
        LiveValue(stringResource(R.string.scan_label_diastolic), diastolic, BoxRole.DIASTOLIC)
        LiveValue(stringResource(R.string.scan_label_pulse), pulse, BoxRole.PULSE)
    }
}

@Composable
private fun LiveValue(label: String, state: LiveValueUiState, role: BoxRole) {
    val unknown = stringResource(R.string.scan_value_unknown)
    val spoken = stringResource(
        R.string.scan_value_content_description,
        label,
        state.value?.toString() ?: stringResource(R.string.scan_value_unknown_content_description),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // The panel changes many times a second; it is not announced, only readable by exploring.
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = roleColor(role))
        Text(
            text = state.value?.toString() ?: unknown,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = if (state.isStable) FontWeight.Bold else FontWeight.Normal,
            color = Color.White,
            modifier = Modifier.alpha(if (state.isStable) 1f else PROVISIONAL_ALPHA),
        )
    }
}
