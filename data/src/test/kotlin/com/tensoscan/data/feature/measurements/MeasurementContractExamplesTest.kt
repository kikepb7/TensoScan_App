package com.tensoscan.data.feature.measurements

import com.tensoscan.data.feature.measurements.remote.MeasurementPageDto
import com.tensoscan.data.feature.measurements.remote.MeasurementRequestDto
import com.tensoscan.data.feature.measurements.remote.MeasurementResponseDto
import com.tensoscan.data.feature.measurements.remote.toRequest
import com.tensoscan.data.feature.measurements.remote.toSyncedEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The app's DTOs against the examples that the backend generates from its own tests (`specs/contracts/examples/`).
 * If the server changes a field, this is where the app notices.
 */
class MeasurementContractExamplesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // Unit tests run with the module directory as working directory.
    private fun example(name: String): JsonObject =
        json.parseToJsonElement(File("../specs/contracts/examples/$name").readText()).jsonObject

    private fun JsonObject.section(key: String): JsonObject = getValue(key).jsonObject

    @Test
    fun `When the contract's create request is read then it matches what the app sends for the same measurement`() {
        val body = example("measurements-create.201.json").section("request").section("body")

        val expected = json.decodeFromJsonElement(MeasurementRequestDto.serializer(), body)
        val stored = entity("5b0b6f0e-6a1f-4d8e-9d0a-2f1c3b7e9a10", source = "CAMERA").copy(
            systolic = expected.systolic,
            diastolic = expected.diastolic,
            pulse = expected.pulse,
            measuredAtEpochMillis = java.time.Instant.parse(expected.measuredAt).toEpochMilli(),
            confidence = expected.recognition?.confidence,
            detectorVersion = expected.recognition?.modelVersion,
            editedFields = "PULSE",
        )

        assertEquals(expected = expected, actual = stored.toRequest())
    }

    @Test
    fun `When the contract's created resource is read then it becomes a synchronised local row`() {
        val body = example("measurements-create.201.json").section("response").section("body")

        val dto = json.decodeFromJsonElement(MeasurementResponseDto.serializer(), body)
        val row = dto.toSyncedEntity()

        assertEquals(expected = "SYNCED", actual = row.syncStatus)
        assertEquals(expected = dto.id, actual = row.remoteId)
        assertEquals(expected = "CAMERA", actual = row.source)
        assertEquals(expected = "PULSE", actual = row.editedFields)
        assertEquals(expected = dto.clientId, actual = row.clientId)
    }

    @Test
    fun `When the idempotent replay is read then it is the same resource as the first answer`() {
        val first = example("measurements-create.201.json").section("response").section("body")
        val replay = example("measurements-create-replay.200.json").section("response").section("body")

        assertEquals(expected = first, actual = replay)
    }

    @Test
    fun `When a page of the list is read then the items and the cursor are understood`() {
        val body = example("measurements-list-page.200.json").section("response").section("body")

        val page = json.decodeFromJsonElement(MeasurementPageDto.serializer(), body)

        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.toSyncedEntity().syncStatus == "SYNCED" })
        assertTrue(page.nextCursor == null || page.nextCursor.isNotBlank())
    }

    @Test
    fun `When the error envelope examples are read then the code is the one the app maps`() {
        val conflict = example("measurements-create-conflict.409.json").section("response").section("body")
        val invalid = example("measurements-create-invalid.422.json").section("response").section("body")

        assertEquals(expected = "CLIENT_ID_CONFLICT", actual = conflict.section("error")["code"].toString().trim('"'))
        assertEquals(expected = "MEASUREMENT_INVALID", actual = invalid.section("error")["code"].toString().trim('"'))
    }
}
