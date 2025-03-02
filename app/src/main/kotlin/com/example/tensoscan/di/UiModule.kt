package com.example.tensoscan.di

import com.example.tensoscan.ui.feature.camera.CameraViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { CameraViewModel(get(), get(), get()) }
}