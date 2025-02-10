package com.example.tensoscan.ui.common.navigation.bottomnavigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.tensoscan.R
import com.example.tensoscan.ui.common.navigation.Routes

sealed class BottomBarItem {
    abstract val route: String
    abstract val title: String
    abstract val icon: @Composable () -> Unit

    data class User(
        override val route: String = Routes.User.route,
        override val title: String = "USUARIO",
        override val icon: @Composable () -> Unit = {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_background),
                contentDescription = "User icon",
                modifier = Modifier.size(32.dp)
            )
        }
    ): BottomBarItem()
}