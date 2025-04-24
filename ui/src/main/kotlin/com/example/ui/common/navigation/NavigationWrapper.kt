package com.example.ui.common.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.common.navigation.Routes.Camera
import com.example.ui.common.navigation.Routes.Login
import com.example.ui.common.navigation.Routes.Register
import com.example.ui.common.navigation.Routes.Summary
import com.example.ui.common.navigation.Routes.User
import com.example.ui.feature.camera.CameraScreenView
import com.example.ui.feature.login.LoginScreen
import com.example.ui.feature.register.RegisterScreen
import com.example.ui.feature.summary.SummaryScreenView
import com.example.ui.feature.user.UserScreenView
import com.example.ui.model.PredictionUiModel
import kotlinx.serialization.json.Json

@RequiresApi(Build.VERSION_CODES.Q)
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
                Json.decodeFromString<List<PredictionUiModel>>(it)
            } ?: emptyList()

            SummaryScreenView(
                mainNavController = mainNavController,
                listPredictionUiModel = listPredictionModel,
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