package com.tensoscan.domain.account

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.error.FieldError
import com.tensoscan.domain.measurement.repository.MeasurementOwnership
import com.tensoscan.domain.measurement.repository.MeasurementRepository
import com.tensoscan.domain.session.SessionRepository
import com.tensoscan.domain.session.SessionState
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val validator: AccountInputValidator, private val repository: AccountRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val problems = validator.validateLogin(email, password)
        if (problems.isNotEmpty()) return Result.failure(problems.toValidation())
        return repository.login(email.trim(), password)
    }
}

/** Creates the account and signs in with it, so the user lands in the app without typing the credentials twice. */
class RegisterAccountUseCase(private val validator: AccountInputValidator, private val repository: AccountRepository) {
    suspend operator fun invoke(account: NewAccount, confirmPassword: String): Result<RegistrationOutcome> {
        val problems = validator.validateRegistration(
            name = account.name,
            lastName = account.lastName,
            email = account.email,
            password = account.password,
            confirmPassword = confirmPassword,
            acceptedPrivacy = account.acceptedPrivacy,
        )
        if (problems.isNotEmpty()) return Result.failure(problems.toValidation())

        val normalised = account.copy(
            name = account.name.trim(),
            lastName = account.lastName.trim(),
            email = account.email.trim(),
        )
        return repository.register(normalised).map {
            // The account exists now; a failed sign-in just sends the user to the login screen.
            if (repository.login(normalised.email, normalised.password).isSuccess) {
                RegistrationOutcome.SIGNED_IN
            } else {
                RegistrationOutcome.CREATED_NOT_SIGNED_IN
            }
        }
    }
}

enum class RegistrationOutcome { SIGNED_IN, CREATED_NOT_SIGNED_IN }

class GetCurrentUserUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(): Result<UserProfile> = repository.currentUser()
}

class ObserveSessionUseCase(private val repository: AccountRepository) {
    operator fun invoke(): Flow<SessionState> = repository.observeSession()
}

/**
 * Signs the user out and removes their measurements from the device. The screen warns first when some of them are not
 * on the server yet (`ObserveUnsyncedCountUseCase`), because this is the one moment they would be lost.
 */
class LogoutUseCase(
    private val accountRepository: AccountRepository,
    private val sessionRepository: SessionRepository,
    private val measurementRepository: MeasurementRepository,
    private val ownership: MeasurementOwnership,
) {
    suspend operator fun invoke() {
        // Revoking the token on the server is a courtesy; being offline must not keep the user signed in.
        accountRepository.revokeSession()
        sessionRepository.clearSession()
        measurementRepository.clearLocal()
        ownership.release()
    }
}

/** Erases the account on the server and then everything of it that is on the device. Needs the password. */
class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val sessionRepository: SessionRepository,
    private val measurementRepository: MeasurementRepository,
    private val ownership: MeasurementOwnership,
) {
    suspend operator fun invoke(password: String): Result<Unit> {
        if (password.isEmpty()) {
            return Result.failure(DomainError.Validation(listOf(FieldError(AccountField.PASSWORD.name, "REQUIRED"))))
        }
        return accountRepository.deleteAccount(password).onSuccess {
            sessionRepository.clearSession()
            measurementRepository.clearLocal()
            ownership.release()
        }
    }
}

private fun Map<AccountField, AccountInputError>.toValidation() =
    DomainError.Validation(map { (field, error) -> FieldError(field.name, error.name) })
