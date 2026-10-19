package com.tensoscan.data.feature.measurements

import com.tensoscan.data.feature.measurements.remote.MeasurementApi
import com.tensoscan.data.feature.measurements.report.ReportFileStore
import com.tensoscan.data.feature.measurements.repository.MeasurementReportRepositoryImpl
import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.domain.error.DomainError
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MeasurementReportRepositoryImplTest {

    private val server = MockWebServer()
    private val saved = mutableMapOf<String, ByteArray>()
    private var storeFailure: Exception? = null
    private val store = object : ReportFileStore {
        override suspend fun save(name: String, content: ByteArray): String {
            storeFailure?.let { throw it }
            saved[name] = content
            return "/cache/reports/$name"
        }
    }

    private val sut by lazy {
        server.start()
        val api = Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MeasurementApi::class.java)
        MeasurementReportRepositoryImpl(api, HttpErrorMapper(testJson), store)
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `When the report is downloaded then it is saved and the file is returned`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("%PDF-1.7").build())

        val actual = sut.downloadPdf("Europe/Madrid").getOrThrow()

        assertEquals(expected = "application/pdf", actual = actual.mimeType)
        assertEquals(expected = "/cache/reports/tensoscan-measurements.pdf", actual = actual.path)
        assertContentEquals(expected = "%PDF-1.7".encodeToByteArray(), actual = saved.values.single())
        val request = server.takeRequest()
        assertEquals(expected = "pdf", actual = request.url.queryParameter("format"))
        assertEquals(expected = "Europe/Madrid", actual = request.url.queryParameter("tz"))
    }

    @Test
    fun `When the server has no measurements to report then nothing is saved`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(404).body("""{"error":{"code":"NOT_FOUND","message":"x"}}""").build(),
        )

        val actual = sut.downloadPdf("UTC").exceptionOrNull()

        assertEquals(expected = DomainError.NotFound, actual = actual)
        assertEquals(expected = 0, actual = saved.size)
    }

    @Test
    fun `When the file cannot be written then it is a storage error and not a network one`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("%PDF-1.7").build())
        storeFailure = IOException("disk full")

        val actual = sut.downloadPdf("UTC").exceptionOrNull()

        assertIs<DomainError.Storage>(actual)
    }
}
