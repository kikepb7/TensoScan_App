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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.tooling.preview.Preview
import com.example.tensoscan.ui.model.BodyDataModel
import com.example.tensoscan.ui.theme.Fontalues
import com.example.tensoscan.ui.theme.SummaryTrackerButtonColor
import com.example.tensoscan.ui.theme.SpacerValues
import com.example.tensoscan.ui.theme.SizeValues

@Composable
fun CardSummaryListItemView(bodyDataModel: BodyDataModel) {
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
                    text = bodyDataModel.date,
                    color = White,
                    fontSize = Fontalues.Font16
                )
                Spacer(modifier = Modifier.height(SizeValues.Size16))
                Text(
                    text = bodyDataModel.bloodPressure + " mmHg",
                    color = White,
                    fontSize = Fontalues.Font16
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                StatusChipView(status = bodyDataModel.status, bodyDataModel.statusColor)
                Spacer(modifier = Modifier.height(SizeValues.Size16))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Estado",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(SizeValues.Size16)
                    )
                    Spacer(modifier = Modifier.width(SizeValues.Size04))
                    Text(
                        text = bodyDataModel.heartRate + " BPM",
                        color = White,
                        fontSize = Fontalues.Font14
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun CardSummaryListItemPreview() {
    CardSummaryListItemView(
        bodyDataModel = BodyDataModel(
            date = "2024-02-14",
            bloodPressure = "120/80",
            heartRate = "72",
            status = "BIEN",
            statusColor = Green
        )
    )
}