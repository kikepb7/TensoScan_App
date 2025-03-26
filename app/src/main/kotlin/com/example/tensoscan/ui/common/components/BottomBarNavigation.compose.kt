package com.example.tensoscan.ui.common.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem
import com.example.tensoscan.ui.theme.primaryBackgroundSummarySetManuallyButtonSelector

@Composable
fun BottomBarNavigation(
    items: List<BottomBarItem>,
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(containerColor = Color.White) {
        items.forEach { item ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = primaryBackgroundSummarySetManuallyButtonSelector,
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Black
                ),
                icon = item.icon,
                label = {
                    Text(text = item.title, color = Color.Black)
                },
                onClick = {
                    navController.navigate(route = item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route = route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            )
        }
    }
}