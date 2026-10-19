package com.tensoscan.domain.account

/** Fields of the sign-in and registration forms. */
enum class AccountField { NAME, LAST_NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD, PRIVACY }

enum class AccountInputError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    PASSWORDS_DO_NOT_MATCH,
    PRIVACY_NOT_ACCEPTED,
}

/**
 * Local validation of the account forms, so obvious mistakes are explained before a request is made. The server
 * validates again (`api-v1.md` §2): the password must have 8 to 128 characters and the email is normalised to lower
 * case.
 */
class AccountInputValidator {

    fun validateLogin(email: String, password: String): Map<AccountField, AccountInputError> = buildMap {
        emailError(email)?.let { put(AccountField.EMAIL, it) }
        if (password.isEmpty()) put(AccountField.PASSWORD, AccountInputError.REQUIRED)
    }

    fun validateRegistration(
        name: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedPrivacy: Boolean,
    ): Map<AccountField, AccountInputError> = buildMap {
        if (name.isBlank()) put(AccountField.NAME, AccountInputError.REQUIRED)
        if (lastName.isBlank()) put(AccountField.LAST_NAME, AccountInputError.REQUIRED)
        emailError(email)?.let { put(AccountField.EMAIL, it) }
        passwordError(password)?.let { put(AccountField.PASSWORD, it) }
        if (!containsKey(AccountField.PASSWORD) && password != confirmPassword) {
            put(AccountField.CONFIRM_PASSWORD, AccountInputError.PASSWORDS_DO_NOT_MATCH)
        }
        if (!acceptedPrivacy) put(AccountField.PRIVACY, AccountInputError.PRIVACY_NOT_ACCEPTED)
    }

    private fun emailError(email: String): AccountInputError? = when {
        email.isBlank() -> AccountInputError.REQUIRED
        !EMAIL_PATTERN.matches(email.trim()) -> AccountInputError.INVALID_EMAIL
        else -> null
    }

    private fun passwordError(password: String): AccountInputError? = when {
        password.isEmpty() -> AccountInputError.REQUIRED
        password.length < MIN_PASSWORD_LENGTH -> AccountInputError.PASSWORD_TOO_SHORT
        password.length > MAX_PASSWORD_LENGTH -> AccountInputError.PASSWORD_TOO_LONG
        else -> null
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 128

        // Deliberately simple: one "@", something on both sides and a dot in the domain. The server is the authority.
        private val EMAIL_PATTERN = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    }
}
