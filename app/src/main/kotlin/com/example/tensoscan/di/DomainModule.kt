package com.example.tensoscan.di

import com.example.tensoscan.domain.feature.permissions.usecase.CheckPermissionsUseCase
import com.example.tensoscan.domain.feature.permissions.usecase.RequestPermissionUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { CheckPermissionsUseCase(get()) }
    factory { RequestPermissionUseCase(get()) }
}