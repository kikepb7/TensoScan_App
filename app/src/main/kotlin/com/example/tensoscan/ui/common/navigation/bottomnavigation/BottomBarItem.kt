package com.example.tensoscan.ui.common.navigation.bottomnavigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
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
                painter = rememberVectorPainter(Icons.Default.Person),
                contentDescription = "User icon",
                modifier = Modifier.size(32.dp)
            )
        }
    ): BottomBarItem()

    data class Camera(
        override val route: String = Routes.Camera.route,
        override val title: String = "CÁMARA",
        override val icon: @Composable () -> Unit = {
            Icon(
                painter = rememberVectorPainter(Icons.Default.CameraAlt),
                contentDescription = "User icon",
                modifier = Modifier.size(32.dp)
            )
        }
    ): BottomBarItem()
}