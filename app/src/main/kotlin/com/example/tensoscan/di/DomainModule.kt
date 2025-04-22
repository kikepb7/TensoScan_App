package com.example.tensoscan.di

import com.example.tensoscan.domain.feature.camera.usecase.UploadImageUseCase
import com.example.tensoscan.domain.feature.login.usecase.LoginUseCase
import com.example.tensoscan.domain.feature.login.usecase.RegisterUserUseCase
import com.example.tensoscan.domain.feature.measurements.usecase.GetMeasurementsUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { UploadImageUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { RegisterUserUseCase(get()) }
    factory { GetMeasurementsUseCase(get()) }
}