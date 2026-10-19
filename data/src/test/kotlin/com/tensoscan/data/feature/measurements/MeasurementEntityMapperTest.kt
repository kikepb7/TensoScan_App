package com.tensoscan.data.feature.measurements

import com.tensoscan.data.feature.measurements.utils.toDomain
import com.tensoscan.data.feature.measurements.utils.toEntity
import com.tensoscan.domain.measurement.model.BloodPressureReading
import com.tensoscan.domain.measurement.model.Measurement
import com.tensoscan.domain.measurement.model.MeasurementId
import com.tensoscan.domain.measurement.model.MeasurementSource
import com.tensoscan.domain.measurement.model.ReadingField
import com.tensoscan.domain.measurement.model.RecognitionMetadata
import com.tensoscan.domain.measurement.model.SyncStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeasurementEntityMapperTest {

    private val measuredAt = Instant.parse("2026-10-19T08:15:00Z")
    private val createdAt = Instant.parse("2026-10-19T08:16:30Z")

    private fun measurement(
        source: MeasurementSource = MeasurementSource.CAMERA,
        syncStatus: SyncStatus = SyncStatus.Pending,
        edited: Set<ReadingField> = setOf(ReadingField.PULSE, ReadingField.SYSTOLIC),
    ) = Measurement(
        id = MeasurementId("client-1"),
        remoteId = null,
        reading = BloodPressureReading(128, 82, 67),
        measuredAt = measuredAt,
        createdAt = createdAt,
        source = source,
        recognition = if (source ==
            MeasurementSource.CAMERA
        ) {
            RecognitionMetadata(0.85f, "mlkit-text-v2", edited)
        } else {
            null
        },
        syncStatus = syncStatus,
    )

    @Test
    fun `When a camera measurement is stored and read back then nothing is lost`() {
        val expected = measurement()

        val actual = expected.toEntity().toDomain()

        assertEquals(expected, actual)
    }

    @Test
    fun `When a manual measurement is stored and read back then it has no recognition data`() {
        val expected = measurement(source = MeasurementSource.MANUAL)

        val entity = expected.toEntity()

        assertNull(entity.confidence)
        assertNull(entity.detectorVersion)
        assertEquals(expected, entity.toDomain())
    }

    @Test
    fun `When fields were edited then they are stored sorted and comma separated`() {
        val entity = measurement().toEntity()

        assertEquals(expected = "SYSTOLIC,PULSE", actual = entity.editedFields)
    }

    @Test
    fun `When no field was edited then the column is empty and reads back as an empty set`() {
        val entity = measurement(edited = emptySet()).toEntity()

        assertEquals(expected = "", actual = entity.editedFields)
        assertEquals(expected = emptySet(), actual = entity.toDomain().recognition?.editedFields)
    }

    @Test
    fun `When a synchronisation failed then the reason is kept`() {
        val expected = measurement(syncStatus = SyncStatus.Failed("MEASUREMENT_INVALID"))

        val entity = expected.toEntity()

        assertEquals(expected = "SYNC_FAILED", actual = entity.syncStatus)
        assertEquals(expected = "MEASUREMENT_INVALID", actual = entity.syncError)
        assertEquals(expected, entity.toDomain())
    }

    @Test
    fun `When a measurement is synchronised then the status column says so`() {
        val entity = measurement(syncStatus = SyncStatus.Synced).toEntity()

        assertEquals(expected = "SYNCED", actual = entity.syncStatus)
        assertNull(entity.syncError)
    }
}
