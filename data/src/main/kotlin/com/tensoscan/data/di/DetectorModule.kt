package com.tensoscan.data.di

import com.tensoscan.data.config.DetectorConfig
import com.tensoscan.data.config.DetectorKind
import com.tensoscan.data.scan.litert.AndroidModelAssets
import com.tensoscan.data.scan.litert.LiteRtDigitDetector
import com.tensoscan.data.scan.litert.ModelAssets
import com.tensoscan.data.scan.mlkit.MlKitDigitDetector
import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.detector.FallbackDigitDetector
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Provider

/**
 * Wires the digit detector. It lives in [ViewModelComponent] so every scanning screen owns its detector: the native
 * resources are released in `onCleared` and two screens never share an interpreter.
 *
 * [DetectorConfig] decides the order: ML Kit first and the in-house model as fallback, or the other way round once the
 * model is validated. Switching is a build setting; nothing else in the app changes. The fallback is prepared only if
 * it exists, so a build without a model simply runs on ML Kit.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object DetectorModule {
    @Provides
    @ViewModelScoped
    fun digitDetector(
        config: DetectorConfig,
        mlKit: Provider<MlKitDigitDetector>,
        liteRt: Provider<LiteRtDigitDetector>,
    ): DigitDetector = when (config.primary) {
        DetectorKind.MLKIT -> FallbackDigitDetector(primary = mlKit.get(), fallback = liteRt.get())
        DetectorKind.LITERT -> FallbackDigitDetector(primary = liteRt.get(), fallback = mlKit.get())
    }
}

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ModelAssetsModule {
    @Binds
    abstract fun modelAssets(impl: AndroidModelAssets): ModelAssets
}
