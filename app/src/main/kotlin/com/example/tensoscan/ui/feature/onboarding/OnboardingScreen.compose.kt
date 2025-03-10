package com.example.tensoscan.ui.feature.onboarding

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dotlottie.dlplayer.Mode
import com.example.tensoscan.ui.common.components.NavigationButtons
import com.example.tensoscan.ui.model.OnboardingModel
import com.example.tensoscan.ui.theme.Fontalues.Font20
import com.example.tensoscan.ui.theme.Fontalues.Font44
import com.example.tensoscan.ui.theme.SizeValues
import com.example.tensoscan.ui.theme.SizeValues.Size16
import com.example.tensoscan.ui.theme.SizeValues.Size45
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.lottiefiles.dotlottie.core.compose.runtime.DotLottieController
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreenView(navController: NavController) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = OnboardingModel.pages.size)

    val animationSources = remember {
        OnboardingModel.pages.map { page ->
            val jsonString = loadJsonFromRaw(context, page.animation)
            DotLottieSource.Json(jsonString)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.wrapContentSize()
        ) { currentPage ->

            val page = OnboardingModel.pages[currentPage]

            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(SizeValues.Size26),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val dotLottieController = remember { DotLottieController() }

                DotLottieAnimation(
                    controller = dotLottieController,
                    modifier = Modifier.size(250.dp),
                    source = animationSources[currentPage],
                    autoplay = true,
//                    loop = true,
                    useFrameInterpolation = true,
                    playMode = Mode.FORWARD
                )

                Text(
                    text = stringResource(page.title),
                    textAlign = TextAlign.Center,
                    fontSize = Font44,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Size16))
                Text(
                    text = stringResource(page.description),
                    textAlign = TextAlign.Center,
                    fontSize = Font20,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Size45)
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    NavigationButtons(
                        navController = navController,
                        pagerState = pagerState,
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun OnboardingPreview() {
    OnboardingScreenView(navController = rememberNavController())
}

fun loadJsonFromRaw(context: Context, resourceId: Int): String {
    val inputStream = context.resources.openRawResource(resourceId)
    return inputStream.bufferedReader().use { it.readText() }
}