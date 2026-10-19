package com.tensoscan.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tensoscan.data.session.AndroidKeystoreTokenCipher
import com.tensoscan.data.session.SessionDataStore
import com.tensoscan.data.session.SessionRepositoryImpl
import com.tensoscan.data.session.TokenCipher
import com.tensoscan.domain.session.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionBindingsModule {
    @Binds
    abstract fun sessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun tokenCipher(impl: AndroidKeystoreTokenCipher): TokenCipher
}

@Module
@InstallIn(SingletonComponent::class)
internal object SessionStoreModule {
    private const val SESSION_FILE = "session"

    @Provides
    @Singleton
    @SessionDataStore
    fun sessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile(SESSION_FILE) })
}
