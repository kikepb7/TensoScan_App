package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tensoscan.ui.common.navigation.Routes.Summary
import com.example.tensoscan.ui.model.OnboardingModel
import com.example.tensoscan.ui.theme.SizeValues.Size16
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun NavigationButtons(navController: NavController, pagerState: PagerState, coroutineScope: CoroutineScope) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Size16),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (pagerState.currentPage > 0) {
            TextButton(onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
            }
        } else {
            Spacer(modifier = Modifier.width(64.dp))
        }

        TextButton(onClick = {
            coroutineScope.launch {
                if (pagerState.currentPage < OnboardingModel.pages.size - 1) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } else {
                    navController.navigate(Summary.route)
                }
            }
        }) {
            if (pagerState.currentPage == OnboardingModel.pages.size - 1) {
                Text(text = "Comenzar")
            } else {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Siguiente")
            }
        }
    }
}