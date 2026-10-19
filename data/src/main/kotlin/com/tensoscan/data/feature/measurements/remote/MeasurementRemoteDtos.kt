package com.tensoscan.data.feature.measurements.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RecognitionDto(
    val confidence: Float,
    @SerialName("model_version") val modelVersion: String,
    @SerialName("edited_fields") val editedFields: List<String>,
)

/** Body of `POST /v1/measurements`: the resource without `id` and `created_at`. */
@Serializable
internal data class MeasurementRequestDto(
    @SerialName("client_id") val clientId: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    @SerialName("measured_at") val measuredAt: String,
    val source: String,
    val recognition: RecognitionDto? = null,
)

@Serializable
internal data class MeasurementResponseDto(
    val id: String,
    @SerialName("client_id") val clientId: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    @SerialName("measured_at") val measuredAt: String,
    @SerialName("created_at") val createdAt: String,
    val source: String,
    val recognition: RecognitionDto? = null,
)

@Serializable
internal data class MeasurementPageDto(
    val items: List<MeasurementResponseDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)
