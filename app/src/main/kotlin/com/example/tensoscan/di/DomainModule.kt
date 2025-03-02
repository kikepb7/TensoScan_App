package com.example.tensoscan.di

import com.example.tensoscan.domain.feature.camera.usecase.UploadImageUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { UploadImageUseCase(get()) }
}