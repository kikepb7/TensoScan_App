package com.tensoscan.data.scan.litert

import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.detector.model.DigitModelSpec
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject

/** Read access to the model files bundled with the app; a seam so the loader is tested without Android. */
internal interface ModelAssets {
    fun list(directory: String): List<String>

    fun read(path: String): ByteArray
}

internal class LoadedModel(val spec: DigitModelSpec, val modelBytes: ByteArray)

/**
 * Finds the newest `digits-<semver>.json` in `assets/models/`, loads the `.tflite` next to it and refuses it unless it is
 * the model the sidecar describes (checksum) and a contract this build understands (major version). Anything wrong is
 * reported as a [DetectorError], which makes the scanner fall back to the other detector instead of reading garbage.
 */
internal class ModelAssetLoader @Inject constructor(private val assets: ModelAssets, private val json: Json) {

    fun load(): Result<LoadedModel> = try {
        Result.success(loadOrThrow())
    } catch (error: DetectorError) {
        Result.failure(error)
    } catch (error: SerializationException) {
        Result.failure(DetectorError.ModelIncompatible)
    } catch (error: IllegalArgumentException) {
        Result.failure(DetectorError.ModelIncompatible)
    } catch (error: java.io.IOException) {
        Result.failure(DetectorError.RuntimeFailure(error))
    }

    private fun loadOrThrow(): LoadedModel {
        val sidecarName = newestSidecar() ?: throw DetectorError.ModelMissing
        val sidecar = json.decodeFromString<ModelSidecarDto>(assets.read("$DIRECTORY/$sidecarName").decodeToString())
        val spec = sidecar.toSpec() ?: throw DetectorError.ModelIncompatible
        if (spec.contractMajor != DigitModelSpec.SUPPORTED_CONTRACT_MAJOR) throw DetectorError.ModelIncompatible

        val modelName = sidecarName.removeSuffix(SIDECAR_EXTENSION) + MODEL_EXTENSION
        if (modelName !in assets.list(DIRECTORY)) throw DetectorError.ModelMissing
        val bytes = assets.read("$DIRECTORY/$modelName")
        if (!sha256Of(bytes).equals(spec.sha256, ignoreCase = true)) throw DetectorError.ModelIncompatible
        return LoadedModel(spec, bytes)
    }

    private fun newestSidecar(): String? = assets
        .list(DIRECTORY)
        .filter { it.startsWith(PREFIX) && it.endsWith(SIDECAR_EXTENSION) }
        .maxWithOrNull(compareBy({ versionOf(it).major }, { versionOf(it).minor }, { versionOf(it).patch }))

    private fun versionOf(fileName: String): Version {
        val parts = fileName.removePrefix(PREFIX).removeSuffix(SIDECAR_EXTENSION).substringBefore('-').split('.')
        return Version(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0,
        )
    }

    private fun sha256Of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private data class Version(val major: Int, val minor: Int, val patch: Int)

    companion object {
        const val DIRECTORY = "models"
        private const val PREFIX = "digits-"
        private const val SIDECAR_EXTENSION = ".json"
        private const val MODEL_EXTENSION = ".tflite"
    }
}
