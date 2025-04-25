package com.example.data.di

import androidx.room.Room
import com.example.data.feature.camera.repository.CameraRepositoryImpl
import com.example.data.feature.camera.service.ImageService
import com.example.data.feature.chatbot.datasource.ChatbotRemoteDataSource
import com.example.data.feature.chatbot.repository.ChatbotRepositoryImpl
import com.example.data.feature.chatbot.service.ChatbotService
import com.example.data.feature.login.repository.LoginRepositoryImpl
import com.example.data.feature.login.service.LoginService
import com.example.data.feature.measurements.database.MeasurementsDatabase
import com.example.data.feature.measurements.database.dao.MeasurementDao
import com.example.data.feature.measurements.datasource.MeasurementDatabaseDatasource
import com.example.data.feature.measurements.datasource.MeasurementRemoteDataSource
import com.example.data.feature.measurements.repository.MeasurementsRepositoryImpl
import com.example.data.feature.measurements.service.MeasurementsService
import com.example.data.local.TokenManager
import com.example.domain.feature.camera.repository.CameraRepository
import com.example.domain.feature.chatbot.repository.ChatbotRepository
import com.example.domain.feature.login.repository.LoginRepository
import com.example.domain.feature.measurements.repository.MeasurementsRepository
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

    single { TokenManager(androidApplication()) }

    single { Room.databaseBuilder(get(), MeasurementsDatabase::class.java, "measurement_db")
        .fallbackToDestructiveMigration(false)
        .build() }

    single<MeasurementDao> { get<MeasurementsDatabase>().getMeasurementDao() }

    single { get<Retrofit>().create(ImageService::class.java)}

    single { get<Retrofit>().create(LoginService::class.java)}

    single { get<Retrofit>().create(MeasurementsService::class.java)}

    single { get<Retrofit>().create(ChatbotService::class.java)}

    factory<CameraRepository> { CameraRepositoryImpl(androidApplication(), get(), get()) }

    factory<LoginRepository> { LoginRepositoryImpl(get(), get()) }

    single { MeasurementRemoteDataSource(get(), get()) }

    single { MeasurementDatabaseDatasource(get()) }

    single { ChatbotRemoteDataSource(get(), get()) }

    single<ChatbotRepository> { ChatbotRepositoryImpl(get()) }

    single<MeasurementsRepository> { MeasurementsRepositoryImpl(get(), get()) }
}