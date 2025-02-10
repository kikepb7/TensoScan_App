package com.example.tensoscan.ui.common.navigation

sealed class Routes(val route: String) {
    data object Home: Routes("home")
    data object User: Routes("user")
}
