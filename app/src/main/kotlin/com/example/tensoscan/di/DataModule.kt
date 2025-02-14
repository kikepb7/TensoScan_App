package com.example.tensoscan.di

import android.app.Application
import android.content.Context
import com.example.tensoscan.ui.utils.PermissionManager
import com.example.tensoscan.data.datasource.feature.camera.repository.CameraRepositoryImpl
import com.example.tensoscan.domain.feature.camera.repository.CameraRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val dataModule = module {
    single {
        HttpClient {
            install(plugin = ContentNegotiation){
                json(json = Json { ignoreUnknownKeys = true }, contentType = ContentType.Any)
            }
            install(plugin = DefaultRequest) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = ""
                }
            }
        }
    }

//    factoryOf(::ApiService)
    factory<CameraRepository> { CameraRepositoryImpl(application = get<Context>() as Application) }
    single { PermissionManager() }
}