package com.tensoscan.data.di

import com.tensoscan.data.scan.mlkit.MlKitDigitDetector
import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.detector.FallbackDigitDetector
import com.tensoscan.domain.scan.detector.UnavailableDigitDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Wires the digit detector. It lives in [ViewModelComponent] so every scanning screen owns its detector: the native
 * resources are released in `onCleared` and two screens never share an interpreter.
 *
 * ML Kit is the primary detector for now. When the in-house LiteRT model ships it becomes the primary one and ML Kit
 * moves to the fallback position; nothing else in the app changes.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object DetectorModule {
    @Provides
    @ViewModelScoped
    fun digitDetector(mlKit: MlKitDigitDetector): DigitDetector =
        FallbackDigitDetector(primary = mlKit, fallback = UnavailableDigitDetector())
}
