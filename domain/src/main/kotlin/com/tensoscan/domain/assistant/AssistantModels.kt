package com.tensoscan.domain.assistant

enum class AssistantRole { USER, ASSISTANT }

data class AssistantMessage(val role: AssistantRole, val content: String)

/** Limits of `POST /v1/assistant/messages` (`api-v1.md` §4). */
object AssistantLimits {
    const val MAX_CHARACTERS = 2_000
    const val MAX_HISTORY = 20
}
