package com.example.ui.common.components

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.ui.common.extensions.formattedConfidence
import com.example.ui.common.extensions.getStatusColor
import com.example.ui.common.extensions.getStatusLabel
import com.example.ui.model.PredictionUiModel
import com.example.ui.theme.CardBackgroundColor
import com.example.ui.theme.CardLabelTextColor
import com.example.ui.theme.ElevationValues.Elevation04
import com.example.ui.theme.PrimaryTextColor
import com.example.ui.theme.SecondaryTextColor
import com.example.ui.theme.SizeValues.Size04
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size12
import com.example.ui.theme.SizeValues.Size18
import com.example.ui.theme.SizeValues.Size20
import com.example.ui.theme.TensoScanTypography
import com.example.ui.R.string as RString

@Composable
fun SummaryCardListItemView(
    predictionUiModel: PredictionUiModel,
    onDelete: () -> Unit
) {
    val highPressure = predictionUiModel.highPressure.toIntOrNull() ?: 0
    val statusColor = getStatusColor(highPressure)
    val statusText = getStatusLabel(highPressure)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = Size08, horizontal = Size12),
        shape = RoundedCornerShape(Size20),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Elevation04),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Size20)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(RString.blood_pressure_text),
                        style = TensoScanTypography.bodyLarge,
                        color = CardLabelTextColor
                    )
                    Spacer(modifier = Modifier.height(Size04))
                    Text(
                        text = stringResource(
                            RString.high_tension_low_tension_text,
                            predictionUiModel.highPressure,
                            predictionUiModel.lowPressure
                        ),
                        style = TensoScanTypography.headlineSmall,
                        color = PrimaryTextColor
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(text = statusText, style = TensoScanTypography.bodySmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f),
                        labelColor = statusColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(Size18))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoItem(
                    label = stringResource(RString.pulse_text),
                    value = stringResource(RString.pulse_bpm_text, predictionUiModel.pulse),
                    icon = Icons.Default.Favorite,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                InfoItem(
                    label = stringResource(RString.accuracy_text),
                    value = predictionUiModel.formattedConfidence() + "%",
                    icon = Icons.Default.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(RString.remove_card_icon_content_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, icon: ImageVector, iconTint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = Size08)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(Size20)
            )
            Spacer(modifier = Modifier.width(Size04))
            Text(
                text = value,
                style = TensoScanTypography.bodyMedium,
                color = PrimaryTextColor
            )
        }
        Spacer(modifier = Modifier.height(Size04))
        Text(
            text = label,
            style = TensoScanTypography.labelMedium,
            color = SecondaryTextColor
        )
    }
}
