package com.tensoscan.ui.feature.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tensoscan.ui.R

/**
 * Temporary destination for the camera tab. The legacy photo capture was removed (it saved pictures in the public
 * gallery and uploaded them to the server); the real-time scanner replaces this screen in the scan feature.
 */
@Composable
internal fun CameraPlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.scan_coming_soon_text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraPlaceholderScreenPreview() {
    CameraPlaceholderScreen()
}
