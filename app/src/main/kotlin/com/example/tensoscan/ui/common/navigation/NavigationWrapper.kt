package com.example.tensoscan.ui.common.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tensoscan.ui.feature.home.HomeScreenView
import com.example.tensoscan.ui.feature.user.UserView

@Composable
fun NavigationWrapper() {
    val mainNavController = rememberNavController()

    NavHost(navController = mainNavController, startDestination = Routes.Home.route) {
        composable(route = Routes.Home.route) {
            HomeScreenView(mainNavController = mainNavController)
        }

        composable(route = Routes.User.route) {
            UserView()
        }
    }
}