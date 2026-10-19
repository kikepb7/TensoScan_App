package com.tensoscan.data.network

import kotlinx.serialization.Serializable

/** Error body defined by the API contract (`api-v1.md` §1.1). */
@Serializable
internal data class ErrorEnvelopeDto(val error: ErrorBodyDto? = null)

@Serializable
internal data class ErrorBodyDto(
    val code: String? = null,
    val message: String? = null,
    val details: List<ErrorDetailDto> = emptyList(),
)

@Serializable
internal data class ErrorDetailDto(val field: String? = null, val code: String)
