package com.example.tensoscan.di

import com.example.tensoscan.ui.feature.camera.CameraViewModel
import com.example.tensoscan.ui.feature.chatbot.ChatbotViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { CameraViewModel(get(), get(), get()) }

    viewModel { ChatbotViewModel() }
}