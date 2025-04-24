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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.common.extensions.getStatusColor
import com.example.ui.common.extensions.getStatusLabel
import com.example.ui.model.PredictionUiModel
import com.example.ui.theme.ElevationValues.Elevation04
import com.example.ui.theme.Fontalues.Font12
import com.example.ui.theme.Fontalues.Font14
import com.example.ui.theme.Fontalues.Font20
import com.example.ui.theme.SizeValues.Size02
import com.example.ui.theme.SizeValues.Size04
import com.example.ui.theme.SizeValues.Size07
import com.example.ui.theme.SizeValues.Size12
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.SizeValues.Size18
import com.example.ui.theme.SummaryTrackerButtonColor
import com.example.ui.R.string as RString

@Composable
fun SummaryCardListItemView(
    predictionUiModel: PredictionUiModel,
    onDelete: () -> Unit
) {
    val highPressureValue = predictionUiModel.highPressure.toIntOrNull() ?: 0
    val statusColor = getStatusColor(highPressureValue)
    val statusText = getStatusLabel(highPressureValue)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Size07),
        shape = RoundedCornerShape(Size16),
        colors = CardDefaults.cardColors(containerColor = SummaryTrackerButtonColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation04)
    ) {
        Column(modifier = Modifier.padding(Size16)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(RString.blood_pressure_text),
                        color = White,
                        fontSize = Font14
                    )
                    Spacer(modifier = Modifier.height(Size04))
                    Text(
                        text = stringResource(
                            RString.high_tension_low_tension_text,
                            predictionUiModel.highPressure,
                            predictionUiModel.lowPressure
                        ),
                        color = White,
                        fontSize = Font20,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusChipView(status = statusText, color = statusColor)
            }

            Spacer(modifier = Modifier.height(Size12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = stringResource(RString.pulse_text),
                    value = stringResource(RString.pulse_bpm_text, predictionUiModel.pulse),
                    icon = Icons.Default.Favorite,
                    iconTint = Color.Red
                )
                InfoItem(
                    label = stringResource(RString.accuracy_text),
                    value = predictionUiModel.confidence + "%",
                    icon = Icons.Default.CheckCircle,
                    iconTint = Green
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(RString.remove_card_icon_content_description),
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(Size18)
            )
            Spacer(modifier = Modifier.width(Size04))
            Text(
                text = value,
                color = White,
                fontSize = Font14
            )
        }
        Spacer(modifier = Modifier.height(Size02))
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = Font12
        )
    }
}

@Composable
@Preview(showBackground = true)
fun CardSummaryListItemPreview() {
    SummaryCardListItemView(
        predictionUiModel = PredictionUiModel(
            highPressure = "160",
            lowPressure = "70",
            pulse = "168",
            confidence = "0.95",
        ),
        onDelete = {}
    )
}