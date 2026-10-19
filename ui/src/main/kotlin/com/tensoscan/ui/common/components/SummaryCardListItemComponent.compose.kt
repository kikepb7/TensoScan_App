package com.tensoscan.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tensoscan.presentation.summary.MeasurementItemUiState
import com.tensoscan.presentation.summary.SyncUiState
import com.tensoscan.ui.common.extensions.getStatusColor
import com.tensoscan.ui.common.extensions.getStatusLabel
import com.tensoscan.ui.theme.CardBackgroundColor
import com.tensoscan.ui.theme.CardLabelTextColor
import com.tensoscan.ui.theme.ElevationValues.Elevation04
import com.tensoscan.ui.theme.PrimaryTextColor
import com.tensoscan.ui.theme.SecondaryTextColor
import com.tensoscan.ui.theme.SizeValues.Size04
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size12
import com.tensoscan.ui.theme.SizeValues.Size18
import com.tensoscan.ui.theme.SizeValues.Size20
import com.tensoscan.ui.theme.TensoScanTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.tensoscan.ui.R.string as RString

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
    FormatStyle.MEDIUM,
    FormatStyle.SHORT,
)

@Composable
fun SummaryCardListItemView(item: MeasurementItemUiState, onDelete: () -> Unit, onRetry: () -> Unit) {
    val statusColor = getStatusColor(item.systolic)
    val statusText = getStatusLabel(item.systolic)
    val date = Instant.ofEpochMilli(item.measuredAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateFormatter)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = Size08, horizontal = Size12),
        shape = RoundedCornerShape(Size20),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Elevation04),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackgroundColor),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Size20)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = stringResource(RString.blood_pressure_text),
                        style = TensoScanTypography.bodyLarge,
                        color = CardLabelTextColor,
                    )
                    Spacer(modifier = Modifier.height(Size04))
                    Text(
                        text = stringResource(RString.high_tension_low_tension_text, item.systolic, item.diastolic),
                        style = TensoScanTypography.headlineSmall,
                        color = PrimaryTextColor,
                    )
                    Text(text = date, style = TensoScanTypography.bodySmall, color = SecondaryTextColor)
                }

                AssistChip(
                    onClick = {},
                    label = { Text(text = statusText, style = TensoScanTypography.bodySmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f),
                        labelColor = statusColor,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(Size18))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InfoItem(
                    label = stringResource(RString.pulse_text),
                    value = stringResource(RString.pulse_bpm_text, item.pulse),
                    icon = Icons.Default.Favorite,
                    iconTint = MaterialTheme.colorScheme.primary,
                )

                item.confidencePercent?.let {
                    InfoItem(
                        label = stringResource(RString.accuracy_text),
                        value = "$it%",
                        icon = Icons.Default.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.secondary,
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(RString.remove_card_icon_content_description),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SyncStatusRow(item.sync, onRetry)
        }
    }
}

/** Says whether the measurement is safe on the server; nothing is shown once it is. */
@Composable
private fun SyncStatusRow(sync: SyncUiState, onRetry: () -> Unit) {
    when (sync) {
        SyncUiState.Synced -> Unit

        SyncUiState.Pending -> StatusLine(
            icon = Icons.Default.Schedule,
            text = stringResource(RString.summary_sync_pending),
            tint = SecondaryTextColor,
        )

        is SyncUiState.Failed -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusLine(
                icon = Icons.Default.ErrorOutline,
                text = stringResource(RString.summary_sync_failed),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(RString.summary_sync_retry)) }
        }
    }
}

@Composable
private fun StatusLine(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(top = Size08), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(Size20))
        Spacer(modifier = Modifier.width(Size04))
        Text(text = text, style = TensoScanTypography.bodySmall, color = tint)
    }
}

@Composable
private fun InfoItem(label: String, value: String, icon: ImageVector, iconTint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = Size08),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(Size20),
            )
            Spacer(modifier = Modifier.width(Size04))
            Text(text = value, style = TensoScanTypography.bodyMedium, color = PrimaryTextColor)
        }
        Spacer(modifier = Modifier.height(Size04))
        Text(text = label, style = TensoScanTypography.labelMedium, color = SecondaryTextColor)
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardPendingPreview() {
    SummaryCardListItemView(
        item = MeasurementItemUiState("1", 128, 82, 67, 1_760_000_000_000L, 93, SyncUiState.Pending),
        onDelete = {},
        onRetry = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardFailedPreview() {
    SummaryCardListItemView(
        item = MeasurementItemUiState(
            "2",
            150,
            95,
            80,
            1_760_000_000_000L,
            null,
            SyncUiState.Failed("MEASUREMENT_INVALID"),
        ),
        onDelete = {},
        onRetry = {},
    )
}
