package com.tensoscan.domain.assistant

import com.tensoscan.domain.error.DomainError
import com.tensoscan.domain.error.FieldError

interface AssistantRepository {
    /** The assistant's answer. Failures: `RateLimited`, `Server` (provider unavailable), `Network`, `Unauthorized`. */
    suspend fun send(prompt: String, history: List<AssistantMessage>): Result<String>
}

class SendAssistantMessageUseCase(private val repository: AssistantRepository) {
    /**
     * Sends [prompt] with the recent conversation. The history is cut to what the server accepts (the last
     * [AssistantLimits.MAX_HISTORY] messages, each limited in length) instead of failing on a long chat.
     */
    suspend operator fun invoke(prompt: String, history: List<AssistantMessage>): Result<String> {
        val text = prompt.trim()
        if (text.isEmpty() || text.length > AssistantLimits.MAX_CHARACTERS) {
            return Result.failure(
                DomainError.Validation(listOf(FieldError("prompt", if (text.isEmpty()) "REQUIRED" else "TOO_LONG"))),
            )
        }
        val recent = history
            .takeLast(AssistantLimits.MAX_HISTORY)
            .map { it.copy(content = it.content.take(AssistantLimits.MAX_CHARACTERS)) }
        return repository.send(text, recent)
    }
}
