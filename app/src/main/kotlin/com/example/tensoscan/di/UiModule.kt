package com.example.tensoscan.di

import com.example.tensoscan.ui.feature.camera.CameraViewModel
import com.example.tensoscan.ui.feature.permissions.PermissionsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::CameraViewModel)
    viewModel { PermissionsViewModel(get(), get()) }
}