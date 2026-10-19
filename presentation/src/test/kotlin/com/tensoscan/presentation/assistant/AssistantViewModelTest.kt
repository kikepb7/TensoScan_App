package com.tensoscan.presentation.assistant

import com.tensoscan.domain.assistant.AssistantLimits
import com.tensoscan.domain.assistant.AssistantMessage
import com.tensoscan.domain.assistant.AssistantRepository
import com.tensoscan.domain.assistant.AssistantRole
import com.tensoscan.domain.assistant.SendAssistantMessageUseCase
import com.tensoscan.domain.error.DomainError
import com.tensoscan.testing.MainDispatcherExtension
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private var gate: CompletableDeferred<Unit>? = null
    private var result: Result<String> = Result.success("Drink water")
    private val histories = mutableListOf<List<AssistantMessage>>()
    private val repository = object : AssistantRepository {
        override suspend fun send(prompt: String, history: List<AssistantMessage>): Result<String> {
            histories += history
            gate?.await()
            return result
        }
    }

    // Lazy: it must be built after the extension replaced Dispatchers.Main.
    private val sut by lazy { AssistantViewModel(SendAssistantMessageUseCase(repository)) }

    private fun type(text: String) = sut.onAction(AssistantUiAction.DraftChanged(text))

    @Test
    fun `When the draft is blank then it cannot be sent`() = runTest(mainDispatcher.dispatcher) {
        type("   ")

        sut.onAction(AssistantUiAction.SendClicked)

        assertTrue(sut.uiState.value.messages.isEmpty())
        assertTrue(histories.isEmpty())
    }

    @Test
    fun `When a message is sent then the question and the answer join the conversation`() =
        runTest(mainDispatcher.dispatcher) {
            type("  Is 130/85 high?  ")

            sut.onAction(AssistantUiAction.SendClicked)

            val actual = sut.uiState.value
            assertEquals(expected = listOf("Is 130/85 high?", "Drink water"), actual = actual.messages.map { it.text })
            assertEquals(expected = listOf(true, false), actual = actual.messages.map { it.fromUser })
            assertEquals(expected = "", actual = actual.draft)
            assertFalse(actual.isWaitingForAnswer)
        }

    @Test
    fun `When the answer is on its way then the typing indicator shows and sending is blocked`() =
        runTest(mainDispatcher.dispatcher) {
            gate = CompletableDeferred()
            type("Hello")
            sut.onAction(AssistantUiAction.SendClicked)
            type("Another")

            assertTrue(sut.uiState.value.isWaitingForAnswer)
            assertFalse(sut.uiState.value.canSend)

            gate?.complete(Unit)
            assertFalse(sut.uiState.value.isWaitingForAnswer)
        }

    @Test
    fun `When a second message is sent then the first exchange travels as history`() =
        runTest(mainDispatcher.dispatcher) {
            type("one")
            sut.onAction(AssistantUiAction.SendClicked)
            type("two")

            sut.onAction(AssistantUiAction.SendClicked)

            assertEquals(
                expected = listOf(
                    AssistantMessage(AssistantRole.USER, "one"),
                    AssistantMessage(AssistantRole.ASSISTANT, "Drink water"),
                ),
                actual = histories.last(),
            )
        }

    @Test
    fun `When sending fails then the message returns to the draft so it can be sent again`() =
        runTest(mainDispatcher.dispatcher) {
            result = Result.failure(DomainError.RateLimited)
            type("Hello")

            sut.onAction(AssistantUiAction.SendClicked)

            val actual = sut.uiState.value
            assertEquals(expected = AssistantError.RATE_LIMITED, actual = actual.error)
            assertEquals(expected = "Hello", actual = actual.draft)
            assertTrue(actual.messages.isEmpty())

            result = Result.success("Hi")
            sut.onAction(AssistantUiAction.SendClicked)

            assertEquals(expected = listOf("Hello", "Hi"), actual = sut.uiState.value.messages.map { it.text })
            assertEquals(expected = null, actual = sut.uiState.value.error)
        }

    @Test
    fun `When the failure is a network problem or an unavailable provider then each is told apart`() =
        runTest(mainDispatcher.dispatcher) {
            type("Hello")
            val expected = mapOf(
                DomainError.Network to AssistantError.NETWORK,
                DomainError.Server to AssistantError.UNAVAILABLE,
                DomainError.SessionExpired to AssistantError.SESSION_EXPIRED,
            )

            val actual = expected.keys.associateWith {
                result = Result.failure(it)
                sut.onAction(AssistantUiAction.SendClicked)
                sut.uiState.value.error
            }

            assertEquals(expected, actual)
        }

    @Test
    fun `When the draft is longer than the server accepts then it is cut`() = runTest(mainDispatcher.dispatcher) {
        type("x".repeat(AssistantLimits.MAX_CHARACTERS + 50))

        assertEquals(expected = AssistantLimits.MAX_CHARACTERS, actual = sut.uiState.value.draft.length)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())
    }
}
