package com.example.tensoscan.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
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

    factoryOf(::ApiService)
}