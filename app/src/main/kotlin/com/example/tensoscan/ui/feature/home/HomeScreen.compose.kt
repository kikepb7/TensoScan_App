package com.example.tensoscan.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.tensoscan.ui.common.components.BottomBarNavigation
import com.example.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.*

@Composable
fun HomeScreenView(
    mainNavController: NavHostController,
) {
    val navController = rememberNavController()
    val items = listOf(User(), Camera(), Summary())

    Scaffold(
        topBar = {},
        bottomBar = {
            BottomBarNavigation(items = items, navController = mainNavController)
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("HOLA MUNDO")
        }

        Box(modifier = Modifier.padding(paddingValues = padding))
    }
}