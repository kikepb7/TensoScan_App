package com.example.ui.common.navigation.bottomnavigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.common.navigation.Routes

@Composable
fun BottomNavigationWrapper(
    navController: NavHostController,
    mainNavController: NavHostController
) {
    NavHost(navController = navController, startDestination = Routes.Summary.route) {
        composable(route = Routes.User.route) {
//            UserScreenView()
        }
    }
}