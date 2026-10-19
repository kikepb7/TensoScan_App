package com.tensoscan.ui.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tensoscan.presentation.login.LoginArguments
import com.tensoscan.presentation.navigation.ReviewMeasurementRoute
import com.tensoscan.presentation.navigation.ScanRoute
import com.tensoscan.presentation.session.AppSession
import com.tensoscan.ui.common.navigation.Routes.Assistant
import com.tensoscan.ui.common.navigation.Routes.Login
import com.tensoscan.ui.common.navigation.Routes.Register
import com.tensoscan.ui.common.navigation.Routes.Summary
import com.tensoscan.ui.common.navigation.Routes.User
import com.tensoscan.ui.feature.assistant.AssistantScreen
import com.tensoscan.ui.feature.login.LoginScreen
import com.tensoscan.ui.feature.register.RegisterScreen
import com.tensoscan.ui.feature.review.ReviewMeasurementScreen
import com.tensoscan.ui.feature.scan.ScanScreen
import com.tensoscan.ui.feature.summary.SummaryScreenView
import com.tensoscan.ui.feature.user.UserScreenView

/**
 * The app's navigation graph. Where the user is follows the session (REQ-A003-01, 02): the start destination depends on
 * it and every later change of the session (signing in, signing out, a session that expired) moves the user and
 * clears the back stack, so no screen has to navigate on its own after those events.
 */
@Composable
fun NavigationWrapper(session: AppSession) {
    // The splash screen covers the app until the session is known.
    if (session == AppSession.LOADING) return

    val navController = rememberNavController()
    val startDestination = remember { if (session == AppSession.SIGNED_IN) Summary.route else Login.route }

    FollowSession(session, navController)

    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = Login.route) {
            LoginScreen(onNavigateToRegister = { navController.navigate(Register.route) })
        }
        composable(route = Register.route) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onGoToLogin = { email ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(LoginArguments.REGISTERED_EMAIL, email)
                    navController.popBackStack()
                },
            )
        }
        composable(route = User.route) { UserScreenView(mainNavController = navController) }
        composable<ScanRoute> {
            ScanScreen(
                onNavigateToReview = { route ->
                    // The scanner is not part of the back stack once the review opens: "back" returns to where the
                    // user started the measurement.
                    navController.navigate(route) { popUpTo<ScanRoute> { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<ReviewMeasurementRoute> { backStackEntry ->
            ReviewMeasurementScreen(
                route = backStackEntry.toRoute<ReviewMeasurementRoute>(),
                onRetake = {
                    navController.navigate(ScanRoute) { popUpTo<ReviewMeasurementRoute> { inclusive = true } }
                },
                onFinished = { navController.popBackStack() },
            )
        }
        composable(route = Assistant.route) { AssistantScreen(mainNavController = navController) }
        composable(route = Summary.route) { SummaryScreenView(mainNavController = navController) }
    }
}

@Composable
private fun FollowSession(session: AppSession, navController: NavHostController) {
    LaunchedEffect(session) {
        val current = navController.currentDestination?.route ?: return@LaunchedEffect
        val onAccountScreen = current == Login.route || current == Register.route
        val target = when {
            session == AppSession.SIGNED_IN && onAccountScreen -> Summary.route
            session == AppSession.SIGNED_OUT && !onAccountScreen -> Login.route
            else -> return@LaunchedEffect
        }
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}
