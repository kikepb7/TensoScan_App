package com.tensoscan.data.scan.litert

import com.tensoscan.data.config.DetectorConfig
import com.tensoscan.data.config.DetectorKind
import com.tensoscan.domain.error.DetectorError
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModelAssetLoaderTest {

    private val modelBytes = byteArrayOf(1, 2, 3, 4, 5)
    private val sha = MessageDigest.getInstance("SHA-256").digest(modelBytes).joinToString("") { "%02x".format(it) }
    private val files = mutableMapOf<String, ByteArray>()
    private val assets = object : ModelAssets {
        override fun list(directory: String) =
            files.keys.filter { it.startsWith("$directory/") }.map { it.removePrefix("$directory/") }

        override fun read(path: String): ByteArray = files.getValue(path)
    }
    private val sut = ModelAssetLoader(assets, Json { ignoreUnknownKeys = true })

    private fun sidecar(
        version: String = "1.0.0",
        contract: String = "1.0.0",
        sha256: String = sha,
        width: Int = 320,
        dtype: String = "float32",
    ) = """
        {"name":"digit-detector","version":"$version","contract_version":"$contract",
         "input":{"width":$width,"height":320,"channels":3,"dtype":"$dtype","range":[0,1],"letterbox_pad":0.447},
         "output":{"num_candidates":2100,"box_format":"cxcywh_normalized","scores":"sigmoid"},
         "classes":["0","1","2","3","4","5","6","7","8","9","colon","dash","E","r"],
         "recommended":{"score_threshold":0.4,"nms_iou_threshold":0.45,"max_detections":32},
         "sha256":"$sha256","training":{"dataset_version":"x"},"metrics":{"map50_real_test":0.0}}
    """.trimIndent().toByteArray()

    private fun install(
        version: String = "1.0.0",
        sidecar: ByteArray = sidecar(version),
        model: ByteArray = modelBytes,
    ) {
        files["models/digits-$version.json"] = sidecar
        files["models/digits-$version.tflite"] = model
    }

    @Test
    fun `When a valid model is bundled then it is loaded with its spec`() {
        install()

        val actual = sut.load().getOrThrow()

        assertEquals(expected = 2100, actual = actual.spec.numCandidates)
        assertEquals(expected = 320, actual = actual.spec.inputSize)
        assertEquals(expected = 0.4f, actual = actual.spec.scoreThreshold)
        assertEquals(expected = 14, actual = actual.spec.classes.size)
        assertTrue(actual.modelBytes.contentEquals(modelBytes))
    }

    @Test
    fun `When there is no model then it is reported as missing`() {
        assertEquals(expected = DetectorError.ModelMissing, actual = sut.load().exceptionOrNull())
    }

    @Test
    fun `When the sidecar exists but the model file does not then it is reported as missing`() {
        files["models/digits-1.0.0.json"] = sidecar()

        assertEquals(expected = DetectorError.ModelMissing, actual = sut.load().exceptionOrNull())
    }

    @Test
    fun `When the checksum does not match then the model is refused`() {
        install(model = byteArrayOf(9, 9, 9))

        assertEquals(expected = DetectorError.ModelIncompatible, actual = sut.load().exceptionOrNull())
    }

    @Test
    fun `When the contract major version is not supported then the model is refused`() {
        install(sidecar = sidecar(contract = "2.0.0"))

        assertEquals(expected = DetectorError.ModelIncompatible, actual = sut.load().exceptionOrNull())
    }

    @Test
    fun `When a minor contract version is newer then the model is still accepted`() {
        install(sidecar = sidecar(contract = "1.3.0"))

        assertTrue(sut.load().isSuccess)
    }

    @Test
    fun `When the input is not a square float image then the model is refused`() {
        install(sidecar = sidecar(width = 256))
        assertEquals(expected = DetectorError.ModelIncompatible, actual = sut.load().exceptionOrNull())

        install(sidecar = sidecar(dtype = "uint8"))
        assertEquals(expected = DetectorError.ModelIncompatible, actual = sut.load().exceptionOrNull())
    }

    @Test
    fun `When the sidecar is not valid json then the model is refused`() {
        install(sidecar = "not json".toByteArray())

        assertEquals(expected = DetectorError.ModelIncompatible, actual = sut.load().exceptionOrNull())
    }

    @Test
    fun `When several versions are bundled then the newest one is used`() {
        install("1.2.0", sidecar("1.2.0"))
        install("1.10.0", sidecar("1.10.0"))
        install("1.9.3", sidecar("1.9.3"))

        assertEquals(expected = "1.10.0", actual = sut.load().getOrThrow().spec.version)
    }

    @Test
    fun `When reading fails with an IO error then it is a runtime failure`() {
        install()
        val failing = ModelAssetLoader(
            object : ModelAssets {
                override fun list(directory: String) = assets.list(directory)

                override fun read(path: String): ByteArray = throw java.io.IOException("disk")
            },
            Json { ignoreUnknownKeys = true },
        )

        assertIs<DetectorError.RuntimeFailure>(failing.load().exceptionOrNull())
    }

    @Test
    fun `When the build setting names the in-house model then it is the primary detector`() {
        assertEquals(expected = DetectorKind.LITERT, actual = DetectorConfig.fromSetting("litert").primary)
        assertEquals(expected = DetectorKind.LITERT, actual = DetectorConfig.fromSetting("LiteRT").primary)
        assertEquals(expected = DetectorKind.MLKIT, actual = DetectorConfig.fromSetting("mlkit").primary)
        assertEquals(expected = DetectorKind.MLKIT, actual = DetectorConfig.fromSetting(null).primary)
        assertEquals(expected = DetectorKind.MLKIT, actual = DetectorConfig.fromSetting("whatever").primary)
    }
}
