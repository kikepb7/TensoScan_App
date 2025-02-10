package com.example.tensoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.tensoscan.di.dataModule
import com.example.tensoscan.di.domainModule
import com.example.tensoscan.di.initKoin
import com.example.tensoscan.di.uiModule
import com.example.tensoscan.ui.common.navigation.NavigationWrapper
import com.example.tensoscan.ui.theme.TensoScanTheme
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initKoin()

        setContent {
            TensoScanTheme {
                NavigationWrapper()
            }
        }
    }
}
