package com.tensoscan.domain.scan.session

import com.tensoscan.domain.measurement.validation.ReadingValidator
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.ExposureAssessment
import com.tensoscan.domain.scan.model.FrameReading
import com.tensoscan.domain.scan.model.FrameRecognition
import com.tensoscan.domain.scan.model.ScanSessionState
import com.tensoscan.domain.time.AppClock
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class ScanSessionFactoryTest {

    private val sut = ScanSessionFactory(ReadingValidator(AppClock { Instant.parse("2026-10-19T08:00:00Z") }))

    @Test
    fun `When creating sessions then every call returns an independent session`() {
        val detector = DetectorInfo("mlkit-text", "v2")

        val first = sut.create(detector)
        val second = sut.create(detector)

        assertNotSame(illegal = first, actual = second)
    }

    @Test
    fun `When a session starts then it is scanning`() {
        val session = sut.create(DetectorInfo("mlkit-text", "v2"))
        val recognition = FrameRecognition(
            timestampMillis = 0,
            detections = emptyList(),
            reading = FrameReading.empty(),
            exposure = ExposureAssessment(meanLuminance = 0.5f, saturatedRatio = 0f),
        )

        val actual = session.onFrame(recognition)

        assertTrue(actual is ScanSessionState.Scanning)
        assertEquals(expected = null, actual = session.partialReading())
    }
}
