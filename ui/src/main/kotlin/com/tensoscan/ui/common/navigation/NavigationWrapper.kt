package com.tensoscan.ui.common.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tensoscan.ui.common.components.MeasurementsWebViewScreen
import com.tensoscan.ui.common.navigation.Routes.Camera
import com.tensoscan.ui.common.navigation.Routes.Chatbot
import com.tensoscan.ui.common.navigation.Routes.Login
import com.tensoscan.ui.common.navigation.Routes.Register
import com.tensoscan.ui.common.navigation.Routes.Summary
import com.tensoscan.ui.common.navigation.Routes.User
import com.tensoscan.ui.feature.camera.CameraPlaceholderScreen
import com.tensoscan.ui.feature.chatbot.ChatbotScreenView
import com.tensoscan.ui.feature.login.LoginScreen
import com.tensoscan.ui.feature.register.RegisterScreen
import com.tensoscan.ui.feature.summary.SummaryScreenView
import com.tensoscan.ui.feature.user.UserScreenView

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
                mainNavController.navigate(Login.route) {
                    popUpTo(Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable(route = Camera.route) {
            CameraPlaceholderScreen()
        }
        composable(route = Chatbot.route) {
            ChatbotScreenView(mainNavController = mainNavController)
        }
        composable("webview/{htmlContent}") { backStackEntry ->
            val htmlContent = backStackEntry.arguments?.getString("htmlContent") ?: ""
            MeasurementsWebViewScreen(url = htmlContent)
        }
        composable(route = Summary.route) {
            SummaryScreenView(mainNavController = mainNavController)
        }
    }
}
