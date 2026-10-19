package com.tensoscan.domain.assistant

import com.tensoscan.domain.error.DomainError
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SendAssistantMessageUseCaseTest {

    private var sentPrompt: String? = null
    private var sentHistory: List<AssistantMessage>? = null
    private var result: Result<String> = Result.success("answer")
    private val sut = SendAssistantMessageUseCase(
        object : AssistantRepository {
            override suspend fun send(prompt: String, history: List<AssistantMessage>): Result<String> {
                sentPrompt = prompt
                sentHistory = history
                return result
            }
        },
    )

    @Test
    fun `When the prompt is blank then nothing is sent`() = runTest {
        val actual = sut("   ", emptyList())

        assertIs<DomainError.Validation>(actual.exceptionOrNull())
        assertEquals(expected = null, actual = sentPrompt)
    }

    @Test
    fun `When the prompt is too long then nothing is sent`() = runTest {
        val actual = sut("x".repeat(AssistantLimits.MAX_CHARACTERS + 1), emptyList())

        assertIs<DomainError.Validation>(actual.exceptionOrNull())
    }

    @Test
    fun `When the prompt is valid then it is trimmed and the answer returned`() = runTest {
        val actual = sut("  ¿Qué significa 135/85?  ", emptyList())

        assertEquals(expected = "answer", actual = actual.getOrThrow())
        assertEquals(expected = "¿Qué significa 135/85?", actual = sentPrompt)
    }

    @Test
    fun `When the conversation is long then only the last messages are sent, each cut to the limit`() = runTest {
        val history = (1..30).map {
            AssistantMessage(
                if (it % 2 ==
                    0
                ) {
                    AssistantRole.ASSISTANT
                } else {
                    AssistantRole.USER
                },
                "m$it" + "y".repeat(2_500),
            )
        }

        sut("hola", history)

        val sent = checkNotNull(sentHistory)
        assertEquals(expected = AssistantLimits.MAX_HISTORY, actual = sent.size)
        assertEquals(expected = true, actual = sent.first().content.startsWith("m11"))
        assertEquals(expected = true, actual = sent.all { it.content.length == AssistantLimits.MAX_CHARACTERS })
    }

    @Test
    fun `When the server fails then its error is returned`() = runTest {
        result = Result.failure(DomainError.RateLimited)

        assertEquals(expected = DomainError.RateLimited, actual = sut("hola", emptyList()).exceptionOrNull())
    }
}
