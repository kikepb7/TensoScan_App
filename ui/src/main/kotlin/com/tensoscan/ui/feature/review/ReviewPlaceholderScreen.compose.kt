package com.tensoscan.ui.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.ui.R

/**
 * Temporary destination so the scanner can be tried end to end: it shows what was read. It is replaced by the real
 * review screen (edit, validate, confirm) in the next phase of the capture feature.
 */
@Composable
internal fun ReviewPlaceholderScreen(route: ReviewMeasurementRoute, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.review_placeholder_title), style = MaterialTheme.typography.headlineSmall)
        Text("SYS ${route.systolic ?: "—"}   DIA ${route.diastolic ?: "—"}   PUL ${route.pulse ?: "—"}")
        Text(route.source.name, style = MaterialTheme.typography.labelMedium)
        Button(onClick = onBack) { Text(stringResource(R.string.review_placeholder_back)) }
    }
}
