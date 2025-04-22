package com.example.tensoscan.ui.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tensoscan.ui.common.navigation.Routes.*
import com.example.tensoscan.ui.feature.camera.CameraScreenView
import com.example.tensoscan.ui.feature.login.LoginScreen
import com.example.tensoscan.ui.feature.register.RegisterScreen
import com.example.tensoscan.ui.feature.summary.SummaryScreenView
import com.example.tensoscan.ui.feature.user.UserScreenView
import com.example.tensoscan.ui.feature.user.UserViewModel
import com.example.tensoscan.ui.model.PredictionModel
import kotlinx.serialization.json.Json

@Composable
fun NavigationWrapper() {
    val mainNavController = rememberNavController()

    NavHost(navController = mainNavController, startDestination = Login.route) {
        composable(route = Login.route) {
            LoginScreen(navController = mainNavController)
        }
        composable(route = Register.route) {
            RegisterScreen(navController = mainNavController)
        }
        composable(route = User.route) {
            UserScreenView(onLogoutNavigate = {
                mainNavController.navigate(Login.route) { popUpTo(0) }
            })
        }

        composable(route = Camera.route) {
            CameraScreenView()
        }

        composable(
            route = Summary.route
        ) { backStackEntry ->
            val jsonBodyDataModel = backStackEntry.arguments?.getString("bodyDataModel")
            val listPredictionModel = jsonBodyDataModel?.let {
                Json.decodeFromString<List<PredictionModel>>(it)
            } ?: emptyList()

            SummaryScreenView(
                mainNavController = mainNavController,
                listPredictionModel = listPredictionModel,
                onSetManually = {}
            )
        }
    }
}


@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavController): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(parentEntry)
}