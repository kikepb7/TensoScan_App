package com.tensoscan.ui.feature.scan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tensoscan.presentation.scan.ScanUnavailableReason
import com.tensoscan.ui.R

/** Full-screen message with a primary and a secondary action, used by the permission and unavailable states. */
@Composable
private fun MessageContent(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) { Text(primaryLabel) }
        OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) { Text(secondaryLabel) }
    }
}

@Composable
internal fun PermissionRequiredContent(
    isPermanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageContent(
        title = stringResource(R.string.scan_permission_title),
        message = stringResource(
            if (isPermanentlyDenied) {
                R.string.scan_permission_denied_permanently
            } else {
                R.string.scan_permission_rationale
            },
        ),
        primaryLabel = stringResource(
            if (isPermanentlyDenied) R.string.scan_open_settings else R.string.scan_permission_grant,
        ),
        onPrimary = if (isPermanentlyDenied) onOpenSettings else onGrant,
        secondaryLabel = stringResource(R.string.scan_enter_manually),
        onSecondary = onEnterManually,
        modifier = modifier,
    )
}

@Composable
internal fun UnavailableContent(
    reason: ScanUnavailableReason,
    onRetry: () -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRecognition = reason == ScanUnavailableReason.RECOGNITION_UNAVAILABLE
    MessageContent(
        title = stringResource(
            if (isRecognition) R.string.scan_unavailable_recognition_title else R.string.scan_unavailable_camera_title,
        ),
        message = stringResource(
            when (reason) {
                ScanUnavailableReason.CAMERA_ERROR -> R.string.scan_unavailable_camera_message
                ScanUnavailableReason.CAMERA_IN_USE -> R.string.scan_unavailable_camera_in_use_message
                ScanUnavailableReason.RECOGNITION_UNAVAILABLE -> R.string.scan_unavailable_recognition_message
            },
        ),
        primaryLabel = stringResource(if (isRecognition) R.string.scan_enter_manually else R.string.scan_retry),
        onPrimary = if (isRecognition) onEnterManually else onRetry,
        secondaryLabel = stringResource(if (isRecognition) R.string.scan_retry else R.string.scan_enter_manually),
        onSecondary = if (isRecognition) onRetry else onEnterManually,
        modifier = modifier,
    )
}

@Composable
internal fun PreparingBanner(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.scan_preparing),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun PausedOverlay(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.scan_paused_message),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onContinue) { Text(stringResource(R.string.scan_continue)) }
        }
    }
}
