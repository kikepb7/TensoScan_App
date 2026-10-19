package com.tensoscan.data.feature.assistant

import com.tensoscan.data.network.HttpErrorMapper
import com.tensoscan.data.network.suspendRunCatching
import com.tensoscan.domain.assistant.AssistantMessage
import com.tensoscan.domain.assistant.AssistantRepository
import com.tensoscan.domain.assistant.AssistantRole
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject

internal interface AssistantApi {
    @POST("v1/assistant/messages")
    suspend fun send(@Body body: AssistantRequestDto): AssistantResponseDto
}

@Serializable
internal data class AssistantRequestDto(val prompt: String, val history: List<AssistantMessageDto>)

@Serializable
internal data class AssistantMessageDto(val role: String, val content: String)

@Serializable
internal data class AssistantResponseDto(val response: String)

internal class AssistantRepositoryImpl @Inject constructor(
    private val api: AssistantApi,
    private val errors: HttpErrorMapper,
) : AssistantRepository {
    override suspend fun send(prompt: String, history: List<AssistantMessage>): Result<String> =
        errors.suspendRunCatching {
            api
                .send(
                    AssistantRequestDto(
                        prompt = prompt,
                        history = history.map { AssistantMessageDto(it.role.wireName(), it.content) },
                    ),
                ).response
        }

    private fun AssistantRole.wireName() = when (this) {
        AssistantRole.USER -> "user"
        AssistantRole.ASSISTANT -> "assistant"
    }
}
