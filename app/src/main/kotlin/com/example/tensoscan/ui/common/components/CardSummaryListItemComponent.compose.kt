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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tensoscan.ui.theme.SummaryTrackerButtonColor

data class BodyDataModel(
    val date: String,
    val bloodPressure: String,
    val heartRate: String,
    val status: String,
    val statusColor: Color
)

@Composable
fun CardSummaryListItemView(bodyDataModel: BodyDataModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SummaryTrackerButtonColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bodyDataModel.date,
                    color = White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = bodyDataModel.bloodPressure + " mmHg",
                    color = White,
                    fontSize = 16.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                StatusChipView(status = bodyDataModel.status, bodyDataModel.statusColor)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Estado",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = bodyDataModel.heartRate + " BPM",
                        color = White,
                        fontSize = 14.sp
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