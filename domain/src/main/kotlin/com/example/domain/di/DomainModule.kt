package com.example.domain.di

import com.example.domain.feature.camera.usecase.SavePhotoUseCase
import com.example.domain.feature.camera.usecase.UploadImageUseCase
import com.example.domain.feature.login.usecase.LoginUseCase
import com.example.domain.feature.login.usecase.RegisterUserUseCase
import com.example.domain.feature.measurements.usecase.GetMeasurementsUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { UploadImageUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { RegisterUserUseCase(get()) }
    factory { GetMeasurementsUseCase(get()) }
    factory { SavePhotoUseCase(get()) }
}