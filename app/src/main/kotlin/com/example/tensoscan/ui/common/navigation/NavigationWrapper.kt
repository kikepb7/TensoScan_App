package com.example.tensoscan.ui.common.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tensoscan.ui.common.components.BodyDataModel
import com.example.tensoscan.ui.common.navigation.Routes.*
import com.example.tensoscan.ui.feature.camera.CameraScreenView
import com.example.tensoscan.ui.feature.home.HomeScreenView
import com.example.tensoscan.ui.feature.summary.SummaryScreenView
import com.example.tensoscan.ui.feature.user.UserScreenView
import kotlinx.serialization.json.Json

@Composable
fun NavigationWrapper() {
    val mainNavController = rememberNavController()

    NavHost(navController = mainNavController, startDestination = Home.route) {
        composable(route = Home.route) {
            HomeScreenView(mainNavController = mainNavController)
        }

        composable(route = User.route) {
            UserScreenView()
        }

        composable(route = Camera.route) {
            CameraScreenView()
        }

        composable(
            route = Summary.route
        ) { backStackEntry ->
            val jsonBodyDataModel = backStackEntry.arguments?.getString("bodyDataModel")
            val listBodyDataModel = jsonBodyDataModel?.let {
                Json.decodeFromString<List<BodyDataModel>>(it)
            } ?: emptyList()

            SummaryScreenView(listBodyDataModel = listBodyDataModel, onScanDevice = { mainNavController.navigate(Camera.route) }, onWriteManually = {})
        }
    }
}