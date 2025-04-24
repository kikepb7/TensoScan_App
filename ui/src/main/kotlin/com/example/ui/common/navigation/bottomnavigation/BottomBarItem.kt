package com.example.ui.common.navigation.bottomnavigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.ui.common.navigation.Routes
import com.example.ui.theme.SizeValues

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
                modifier = Modifier.size(SizeValues.Size32)
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
                modifier = Modifier.size(SizeValues.Size32)
            )
        }
    ): BottomBarItem()

    data class Summary(
        override val route: String = Routes.Summary.route,
        override val title: String = "HISTORIAL",
        override val icon: @Composable () -> Unit = {
            Icon(
                painter = rememberVectorPainter(Icons.Default.History),
                contentDescription = "Historical icon",
                modifier = Modifier.size(SizeValues.Size32)
            )
        }
    ): BottomBarItem()
}