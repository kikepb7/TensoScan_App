package com.example.domain.di

import com.example.domain.feature.camera.usecase.SavePhotoUseCase
import com.example.domain.feature.camera.usecase.UploadImageUseCase
import com.example.domain.feature.chatbot.usecase.ChatbotUseCase
import com.example.domain.feature.login.usecase.LoginUseCase
import com.example.domain.feature.login.usecase.RegisterUserUseCase
import com.example.domain.feature.measurements.usecase.DeleteMeasurementUseCase
import com.example.domain.feature.measurements.usecase.GetMeasurementHistoryHtmlUseCase
import com.example.domain.feature.measurements.usecase.GetMeasurementHistoryPdfUseCase
import com.example.domain.feature.measurements.usecase.GetMeasurementsUseCase
import com.example.domain.feature.session.usecase.LogoutUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { UploadImageUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { RegisterUserUseCase(get()) }
    factory { GetMeasurementsUseCase(get()) }
    factory { SavePhotoUseCase(get()) }
    factory { DeleteMeasurementUseCase(get()) }
    factory { GetMeasurementHistoryHtmlUseCase(get()) }
    factory { GetMeasurementHistoryPdfUseCase(get()) }
    factory { LogoutUseCase(get(), get()) }
    factory { ChatbotUseCase(get()) }
}