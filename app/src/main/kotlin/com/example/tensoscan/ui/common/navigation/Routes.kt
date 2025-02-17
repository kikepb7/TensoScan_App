package com.example.tensoscan.ui.common.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Routes(val route: String) {
    data object Home: Routes("home")
    data object User: Routes("user")
    data object Camera: Routes("camera")
    data object Summary: Routes("summary")
}
