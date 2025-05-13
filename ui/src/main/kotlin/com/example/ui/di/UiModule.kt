package com.example.ui.di

import com.example.ui.feature.camera.CameraViewModel
import com.example.ui.feature.camera.PermissionViewModel
import com.example.ui.feature.chatbot.ChatbotViewModel
import com.example.ui.feature.login.LoginViewModel
import com.example.ui.feature.register.RegisterViewModel
import com.example.ui.feature.summary.SummaryViewModel
import com.example.ui.feature.user.UserViewModel
import com.example.ui.utils.PermissionManager
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { CameraViewModel(get(), get()) }
    viewModel { PermissionViewModel(get()) }
    viewModel { SummaryViewModel(get(), get(), get(), get(), get()) }
    viewModel { UserViewModel(get(), get()) }
    viewModel { ChatbotViewModel(get()) }
    single { PermissionManager() }
}