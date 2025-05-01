package com.example.ui.common.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem
import com.example.ui.theme.BottomBarContainerColor
import com.example.ui.theme.BottomBarIndicatorColor
import com.example.ui.theme.BottomBarSelectedIconColor
import com.example.ui.theme.BottomBarUnselectedIconColor
import com.example.ui.theme.TensoScanTypography

@Composable
fun BottomBarNavigation(
    items: List<BottomBarItem>,
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(containerColor = BottomBarContainerColor) {
        items.forEach { item ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = BottomBarIndicatorColor,
                    selectedIconColor = BottomBarSelectedIconColor,
                    unselectedIconColor = BottomBarUnselectedIconColor
                ),
                icon = item.icon,
                label = {
                    Text(
                        text = item.title,
                        style = TensoScanTypography.labelMedium,
                        color = if (currentDestination?.hierarchy?.any { it.route == item.route } == true)
                            BottomBarSelectedIconColor
                        else
                            BottomBarUnselectedIconColor
                    )
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