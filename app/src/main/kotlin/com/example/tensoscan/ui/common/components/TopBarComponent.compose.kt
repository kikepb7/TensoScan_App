package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.tensoscan.ui.model.TopBarModel
import com.example.tensoscan.ui.theme.BorderValues.Border01
import com.example.tensoscan.ui.theme.Fontalues.Font20
import com.example.tensoscan.ui.theme.SizeValues.Size40
import com.example.tensoscan.ui.theme.TopBarColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarView(topAppBarModel: TopBarModel) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(topAppBarModel.title),
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = Font20,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        navigationIcon = {
            Image(
                painter = painterResource(id = topAppBarModel.image),
                contentDescription = "User image",
                modifier = Modifier
                    .size(Size40)
                    .clip(CircleShape)
                    .border(Border01, Color.Black, CircleShape)
            )
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = topAppBarModel.icon,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopBarColor,
            titleContentColor = Color.White
        )
    )
}