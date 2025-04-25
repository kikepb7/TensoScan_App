package com.example.ui.common.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Routes(val route: String) {
    data object Login: Routes("login")
    data object Register: Routes("register")
    data object User: Routes("user")
    data object Camera: Routes("camera")
    data object Summary: Routes("summary")
    data object Chatbot: Routes("chatbot")
}
