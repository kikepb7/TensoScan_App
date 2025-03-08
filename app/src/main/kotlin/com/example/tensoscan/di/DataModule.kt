package com.example.tensoscan.di

import com.example.tensoscan.data.common.Constants.GEMINI_API_KEY
import com.example.tensoscan.ui.utils.PermissionManager
import com.example.tensoscan.data.feature.camera.repository.CameraRepositoryImpl
import com.example.tensoscan.data.feature.camera.service.ImageApiService
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val dataModule = module {

    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(get())
            .build()
    }

    single { get<Retrofit>().create(ImageApiService::class.java)}

    factory<CameraRepository> { CameraRepositoryImpl(androidApplication(), get()) }

    single { PermissionManager() }

}