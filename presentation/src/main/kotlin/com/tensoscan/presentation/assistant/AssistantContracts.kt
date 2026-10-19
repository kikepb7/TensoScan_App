package com.tensoscan.presentation.assistant

import com.tensoscan.domain.assistant.AssistantLimits

data class ChatMessageUiState(val id: Long, val fromUser: Boolean, val text: String)

enum class AssistantError { RATE_LIMITED, NETWORK, UNAVAILABLE, SESSION_EXPIRED, UNKNOWN }

data class AssistantUiState(
    val messages: List<ChatMessageUiState> = emptyList(),
    val draft: String = "",
    /** The answer is on its way: the screen shows the typing indicator. */
    val isWaitingForAnswer: Boolean = false,
    /** The last send failed. The message was taken back into [draft], so retrying is sending again. */
    val error: AssistantError? = null,
) {
    val canSend: Boolean get() = !isWaitingForAnswer && draft.isNotBlank()
    val maxCharacters: Int get() = AssistantLimits.MAX_CHARACTERS
}

sealed interface AssistantUiAction {
    data class DraftChanged(val value: String) : AssistantUiAction

    data object SendClicked : AssistantUiAction

    data object ErrorDismissed : AssistantUiAction
}
