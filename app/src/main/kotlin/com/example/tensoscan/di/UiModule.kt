package com.example.tensoscan.di

import com.example.tensoscan.ui.feature.camera.CameraViewModel
import com.example.tensoscan.ui.feature.camera.PermissionViewModel
import com.example.tensoscan.ui.feature.summary.SummaryViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { CameraViewModel(get()) }
    viewModel { PermissionViewModel(get()) }
    viewModel { SummaryViewModel(get()) }
}