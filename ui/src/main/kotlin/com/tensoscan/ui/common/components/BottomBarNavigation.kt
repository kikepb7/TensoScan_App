package com.tensoscan.ui.common.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tensoscan.presentation.navigation.ScanRoute
import com.tensoscan.ui.common.navigation.Routes
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem
import com.tensoscan.ui.theme.BottomBarContainerColor
import com.tensoscan.ui.theme.BottomBarIndicatorColor
import com.tensoscan.ui.theme.BottomBarSelectedIconColor
import com.tensoscan.ui.theme.BottomBarUnselectedIconColor
import com.tensoscan.ui.theme.SizeValues
import com.tensoscan.ui.theme.TensoScanTypography

@Composable
internal fun BottomBarNavigation(items: List<BottomBarItem>, navController: NavHostController) {
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
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(SizeValues.Size32),
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.titleRes),
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
                        // The history is always the root of a signed-in session (see `NavigationWrapper`).
                        popUpTo(route = Routes.Summary.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
            )
        }
    }
}
