package com.tensoscan.ui.common.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tensoscan.presentation.navigation.ScanRoute
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem
import com.tensoscan.ui.theme.BottomBarContainerColor
import com.tensoscan.ui.theme.BottomBarIndicatorColor
import com.tensoscan.ui.theme.BottomBarSelectedIconColor
import com.tensoscan.ui.theme.BottomBarUnselectedIconColor
import com.tensoscan.ui.theme.TensoScanTypography

@Composable
fun BottomBarNavigation(items: List<BottomBarItem>, navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(containerColor = BottomBarContainerColor) {
        items.forEach { item ->
            NavigationBarItem(
                colors =
                NavigationBarItemDefaults.colors(
                    indicatorColor = BottomBarIndicatorColor,
                    selectedIconColor = BottomBarSelectedIconColor,
                    unselectedIconColor = BottomBarUnselectedIconColor,
                ),
                icon = item.icon,
                label = {
                    Text(
                        text = item.title,
                        style = TensoScanTypography.labelMedium,
                        color =
                        if (currentDestination?.hierarchy?.any { it.route == item.route } == true) {
                            BottomBarSelectedIconColor
                        } else {
                            BottomBarUnselectedIconColor
                        },
                    )
                },
                onClick = {
                    if (item is BottomBarItem.Camera) {
                        // The camera is a full-screen flow, not a tab with its own bottom bar.
                        navController.navigate(ScanRoute)
                        return@NavigationBarItem
                    }
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
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
            )
        }
    }
}
