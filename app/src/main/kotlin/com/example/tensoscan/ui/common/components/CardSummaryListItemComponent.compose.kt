package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.tensoscan.R
import com.example.tensoscan.domain.feature.camera.model.PredictionModel
import com.example.tensoscan.ui.theme.Fontalues
import com.example.tensoscan.ui.theme.SummaryTrackerButtonColor
import com.example.tensoscan.ui.theme.SpacerValues
import com.example.tensoscan.ui.theme.SizeValues
import com.example.tensoscan.ui.theme.SizeValues.Size16
import com.example.tensoscan.ui.theme.SizeValues.Size24

@Composable
fun CardSummaryListItemView(
    predictionModel: PredictionModel,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SummaryTrackerButtonColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SpacerValues.Spacer16, horizontal = SpacerValues.Spacer32),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Presión arterial",
                    color = White,
                    fontSize = Fontalues.Font16
                )
                Spacer(modifier = Modifier.height(Size16))
                Text(
                    text = predictionModel.digit.toString() + " mmHg",
                    color = White,
                    fontSize = Fontalues.Font16
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                StatusChipView(status = predictionModel.digit, Green)
                Spacer(modifier = Modifier.height(Size16))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Estado",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(Size16)
                    )
                    Spacer(modifier = Modifier.width(SizeValues.Size04))
                    Text(
                        text = predictionModel.confidence.toString() + " BPM",
                        color = White,
                        fontSize = Fontalues.Font14
                    )
                }
            }

            IconButton(
                modifier = Modifier.padding(start = Size24),
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_card_icon_content_description),
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun CardSummaryListItemPreview() {
    CardSummaryListItemView(
        predictionModel = PredictionModel(
            digit = "168",
            confidence = "0.95"
        ),
        onDelete = {}
    )
}