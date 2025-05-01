package com.example.ui.common.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.ui.R
import com.example.ui.model.TopBarModel
import com.example.ui.theme.BorderValues.Border01
import com.example.ui.theme.SizeValues.Size40
import com.example.ui.theme.TensoScanTypography
import com.example.ui.theme.TopBarColor
import com.example.ui.theme.TopBarTitleColor

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
                    style = TensoScanTypography.headlineSmall,
                    color = TopBarTitleColor
                )
            }
        },
        navigationIcon = {
            Image(
                painter = painterResource(id = topAppBarModel.image),
                contentDescription = stringResource(R.string.user_image_content_description),
                modifier = Modifier
                    .size(Size40)
                    .clip(CircleShape)
                    .border(Border01, TopBarTitleColor, CircleShape)
            )
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = topAppBarModel.icon,
                    contentDescription = stringResource(R.string.settings_content_description),
                    tint = TopBarTitleColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopBarColor,
            titleContentColor = TopBarTitleColor
        )
    )
}