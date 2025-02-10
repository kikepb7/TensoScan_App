package com.example.tensoscan.ui.common.navigation.bottomnavigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tensoscan.ui.common.navigation.Routes
import com.example.tensoscan.ui.feature.user.UserView

@Composable
fun BottomNavigationWrapper(
    navController: NavHostController,
    mainNavController: NavHostController
) {
    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(route = Routes.User.route) {
            UserView()
        }
    }
}