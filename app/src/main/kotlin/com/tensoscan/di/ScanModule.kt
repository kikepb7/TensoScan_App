package com.tensoscan.di

import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.exposure.FrameExposureEvaluator
import com.tensoscan.domain.scan.parser.DisplayLayoutParser
import com.tensoscan.domain.scan.session.ScanSessionFactory
import com.tensoscan.domain.scan.usecase.PrepareDetectorUseCase
import com.tensoscan.domain.scan.usecase.RecognizeFrameUseCase
import com.tensoscan.domain.time.AppClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Scanning pieces of the domain. They live in [ViewModelComponent] because they depend on the detector, which belongs
 * to one scanning screen; the detector binding is view-model scoped, so both use cases share the same instance.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object ScanModule {
    @Provides
    fun prepareDetectorUseCase(detector: DigitDetector) = PrepareDetectorUseCase(detector)

    @Provides
    fun recognizeFrameUseCase(detector: DigitDetector) =
        RecognizeFrameUseCase(detector, DisplayLayoutParser(), FrameExposureEvaluator())

    @Provides
    fun scanSessionFactory(clock: AppClock) = ScanSessionFactory(ReadingValidator(clock))
}
