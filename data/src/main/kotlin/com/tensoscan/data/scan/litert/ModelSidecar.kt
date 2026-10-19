package com.tensoscan.data.scan.litert

import com.tensoscan.domain.scan.detector.model.DigitModelSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `digits-<semver>.json` next to the model (`digit-detector-model.md` §5). Unknown fields are ignored. */
@Serializable
internal data class ModelSidecarDto(
    val name: String,
    val version: String,
    @SerialName("contract_version") val contractVersion: String,
    val input: InputDto,
    val output: OutputDto,
    val classes: List<String>,
    val recommended: RecommendedDto,
    val sha256: String,
) {
    @Serializable
    data class InputDto(val width: Int, val height: Int, val channels: Int, val dtype: String)

    @Serializable
    data class OutputDto(@SerialName("num_candidates") val numCandidates: Int)

    @Serializable
    data class RecommendedDto(
        @SerialName("score_threshold") val scoreThreshold: Float,
        @SerialName("nms_iou_threshold") val nmsIouThreshold: Float,
        @SerialName("max_detections") val maxDetections: Int,
    )

    /** Null when the model is not one this build can run (non-square input, other channels or data type). */
    fun toSpec(): DigitModelSpec? {
        if (input.width != input.height || input.channels != 3 || input.dtype != SUPPORTED_DTYPE) return null
        return DigitModelSpec(
            name = name,
            version = version,
            contractVersion = contractVersion,
            inputSize = input.width,
            numCandidates = output.numCandidates,
            classes = classes,
            scoreThreshold = recommended.scoreThreshold,
            nmsIouThreshold = recommended.nmsIouThreshold,
            maxDetections = recommended.maxDetections,
            sha256 = sha256,
        )
    }
}

// Top level on purpose: a private companion would hide the serializer the plugin generates for the class.
private const val SUPPORTED_DTYPE = "float32"
