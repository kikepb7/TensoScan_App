package com.tensoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tensoscan.presentation.session.AppSession
import com.tensoscan.presentation.session.AppViewModel
import com.tensoscan.ui.common.navigation.NavigationWrapper
import com.tensoscan.ui.theme.TensoScanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash screen stays until the stored session has been read, so the first screen is the right one.
        installSplashScreen().setKeepOnScreenCondition { appViewModel.uiState.value.session == AppSession.LOADING }
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            TensoScanTheme {
                NavigationWrapper(session = state.session)
            }
        }
    }
}
