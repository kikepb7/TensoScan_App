package com.tensoscan.ui.common.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.navigation.ScanRoute
import com.tensoscan.ui.common.navigation.Routes.Chatbot
import com.tensoscan.ui.common.navigation.Routes.Login
import com.tensoscan.ui.common.navigation.Routes.Register
import com.tensoscan.ui.common.navigation.Routes.Summary
import com.tensoscan.ui.common.navigation.Routes.User
import com.tensoscan.ui.feature.chatbot.ChatbotScreenView
import com.tensoscan.ui.feature.login.LoginScreen
import com.tensoscan.ui.feature.register.RegisterScreen
import com.tensoscan.ui.feature.review.ReviewMeasurementScreen
import com.tensoscan.ui.feature.scan.ScanScreen
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
        composable<ScanRoute> {
            ScanScreen(
                onNavigateToReview = { route ->
                    // The scanner is not part of the back stack once the review opens: "back" returns to where the
                    // user started the measurement.
                    mainNavController.navigate(route) { popUpTo<ScanRoute> { inclusive = true } }
                },
                onNavigateBack = { mainNavController.popBackStack() },
            )
        }
        composable<ReviewMeasurementRoute> { backStackEntry ->
            ReviewMeasurementScreen(
                route = backStackEntry.toRoute<ReviewMeasurementRoute>(),
                onRetake = {
                    mainNavController.navigate(ScanRoute) { popUpTo<ReviewMeasurementRoute> { inclusive = true } }
                },
                onFinished = { mainNavController.popBackStack() },
            )
        }
        composable(route = Chatbot.route) {
            ChatbotScreenView(mainNavController = mainNavController)
        }
        composable(route = Summary.route) {
            SummaryScreenView(mainNavController = mainNavController)
        }
    }
}
