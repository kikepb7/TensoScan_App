package com.tensoscan.data.common

import com.tensoscan.domain.error.FailureDomain

/** Legacy HTTP failure carrier used by the history and assistant data sources until they adopt `HttpErrorMapper`. */
internal data class FailureDto(val code: Int, val message: String?)

internal fun FailureDto.toFailureDomain(): FailureDomain = when (code) {
    HTTP_UNAUTHORIZED -> FailureDomain.Unauthorized
    else -> FailureDomain.ApiError
}

private const val HTTP_UNAUTHORIZED = 401
