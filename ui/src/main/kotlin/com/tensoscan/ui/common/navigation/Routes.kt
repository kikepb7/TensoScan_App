package com.tensoscan.ui.common.navigation

sealed class Routes(val route: String) {
    data object Login : Routes("login")

    data object Register : Routes("register")

    data object User : Routes("user")

    data object Camera : Routes("camera")

    data object Summary : Routes("summary")

    data object Assistant : Routes("assistant")
}
