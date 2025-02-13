package com.example.tensoscan.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appContext: Context, config: KoinAppDeclaration? = null) {
    startKoin {
        androidContext(appContext)
        config?.invoke(this)
        modules(
            uiModule,
            domainModule,
            dataModule
        )
    }
}