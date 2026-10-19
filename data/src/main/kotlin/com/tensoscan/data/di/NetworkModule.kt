package com.tensoscan.data.di

import com.tensoscan.data.config.ApiConfig
import com.tensoscan.data.feature.chatbot.service.ChatbotService
import com.tensoscan.data.feature.login.service.AuthApi
import com.tensoscan.data.feature.login.service.UserApi
import com.tensoscan.data.feature.measurements.service.MeasurementsService
import com.tensoscan.data.network.AuthInterceptor
import com.tensoscan.data.network.AuthenticatedApi
import com.tensoscan.data.network.PublicApi
import com.tensoscan.data.network.RefreshApi
import com.tensoscan.data.network.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @PublicApi
    fun publicClient(): OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @AuthenticatedApi
    fun authenticatedClient(
        @PublicApi base: OkHttpClient,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = base
        .newBuilder()
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    @PublicApi
    fun publicRetrofit(@PublicApi client: OkHttpClient, json: Json, config: ApiConfig): Retrofit =
        retrofit(client, json, config)

    @Provides
    @Singleton
    @AuthenticatedApi
    fun authenticatedRetrofit(@AuthenticatedApi client: OkHttpClient, json: Json, config: ApiConfig): Retrofit =
        retrofit(client, json, config)

    @Provides
    @Singleton
    fun authApi(@PublicApi retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun refreshApi(@PublicApi retrofit: Retrofit): RefreshApi = retrofit.create(RefreshApi::class.java)

    @Provides
    @Singleton
    fun userApi(@AuthenticatedApi retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun measurementsService(@AuthenticatedApi retrofit: Retrofit): MeasurementsService =
        retrofit.create(MeasurementsService::class.java)

    @Provides
    @Singleton
    fun chatbotService(@AuthenticatedApi retrofit: Retrofit): ChatbotService =
        retrofit.create(ChatbotService::class.java)

    private fun retrofit(client: OkHttpClient, json: Json, config: ApiConfig): Retrofit = Retrofit
        .Builder()
        .baseUrl(config.baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
        .build()
}
