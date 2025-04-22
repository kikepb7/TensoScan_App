package com.example.tensoscan.di

import com.example.tensoscan.ui.feature.camera.CameraViewModel
import com.example.tensoscan.ui.feature.camera.PermissionViewModel
import com.example.tensoscan.ui.feature.login.LoginViewModel
import com.example.tensoscan.ui.feature.register.RegisterViewModel
import com.example.tensoscan.ui.feature.summary.SummaryViewModel
import com.example.tensoscan.ui.feature.user.UserViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { CameraViewModel(get()) }
    viewModel { PermissionViewModel(get()) }
    viewModel { SummaryViewModel(get(), get()) }
    viewModel { UserViewModel() }
}