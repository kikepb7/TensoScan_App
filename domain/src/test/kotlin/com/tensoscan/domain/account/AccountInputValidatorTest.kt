package com.tensoscan.domain.account

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountInputValidatorTest {

    private val sut = AccountInputValidator()

    private fun register(
        name: String = "Ana",
        lastName: String = "García",
        email: String = "ana@example.com",
        password: String = "supersecret",
        confirm: String = "supersecret",
        privacy: Boolean = true,
    ) = sut.validateRegistration(name, lastName, email, password, confirm, privacy)

    @Test
    fun `When the login form is complete then there are no problems`() {
        assertTrue(sut.validateLogin("ana@example.com", "x").isEmpty())
    }

    @Test
    fun `When the login form is empty then both fields are required`() {
        val actual = sut.validateLogin("", "")

        assertEquals(
            expected = mapOf(
                AccountField.EMAIL to AccountInputError.REQUIRED,
                AccountField.PASSWORD to AccountInputError.REQUIRED,
            ),
            actual = actual,
        )
    }

    @ParameterizedTest(name = "\"{0}\" is not an email")
    @ValueSource(strings = ["ana", "ana@", "@example.com", "ana@example", "a na@example.com", "ana@@example.com"])
    fun `When the email is malformed then it is rejected`(email: String) {
        assertEquals(
            expected = AccountInputError.INVALID_EMAIL,
            actual = sut.validateLogin(email, "x")[AccountField.EMAIL],
        )
    }

    @Test
    fun `When the email has surrounding spaces then it is accepted`() {
        assertTrue(sut.validateLogin("  ana@example.com ", "x").isEmpty())
    }

    @Test
    fun `When the registration is complete then there are no problems`() {
        assertTrue(register().isEmpty())
    }

    @Test
    fun `When names are blank then they are required`() {
        val actual = register(name = " ", lastName = "")

        assertEquals(expected = AccountInputError.REQUIRED, actual = actual[AccountField.NAME])
        assertEquals(expected = AccountInputError.REQUIRED, actual = actual[AccountField.LAST_NAME])
    }

    @Test
    fun `When the password is too short or too long then it is rejected`() {
        assertEquals(
            expected = AccountInputError.PASSWORD_TOO_SHORT,
            actual = register(password = "1234567", confirm = "1234567")[AccountField.PASSWORD],
        )
        val long = "x".repeat(AccountInputValidator.MAX_PASSWORD_LENGTH + 1)
        assertEquals(
            expected = AccountInputError.PASSWORD_TOO_LONG,
            actual = register(password = long, confirm = long)[AccountField.PASSWORD],
        )
    }

    @Test
    fun `When the password has the limit lengths then it is accepted`() {
        assertTrue(register(password = "12345678", confirm = "12345678").isEmpty())
        val long = "x".repeat(AccountInputValidator.MAX_PASSWORD_LENGTH)
        assertTrue(register(password = long, confirm = long).isEmpty())
    }

    @Test
    fun `When the confirmation differs then only the confirmation is flagged`() {
        val actual = register(confirm = "different")

        assertEquals(
            expected = mapOf(AccountField.CONFIRM_PASSWORD to AccountInputError.PASSWORDS_DO_NOT_MATCH),
            actual = actual,
        )
    }

    @Test
    fun `When the privacy notice is not accepted then registration is refused`() {
        assertEquals(
            expected = mapOf(AccountField.PRIVACY to AccountInputError.PRIVACY_NOT_ACCEPTED),
            actual = register(privacy = false),
        )
    }
}
