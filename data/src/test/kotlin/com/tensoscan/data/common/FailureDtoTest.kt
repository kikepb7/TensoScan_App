package com.tensoscan.data.common

import com.tensoscan.domain.error.FailureDomain
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class FailureDtoTest {

    @TestFactory
    fun `When a status code is mapped then the legacy failure matches`() = listOf(
        400 to FailureDomain.ApiError,
        401 to FailureDomain.Unauthorized,
        500 to FailureDomain.ApiError,
    ).map { (code, expected) ->
        dynamicTest("HTTP $code") {
            val actual = FailureDto(code = code, message = null).toFailureDomain()

            assertEquals(expected = expected, actual = actual)
        }
    }
}
