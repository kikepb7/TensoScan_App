package com.tensoscan.ui.feature.assistant

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tensoscan.presentation.assistant.AssistantError
import com.tensoscan.presentation.assistant.AssistantUiAction
import com.tensoscan.presentation.assistant.AssistantUiState
import com.tensoscan.presentation.assistant.AssistantViewModel
import com.tensoscan.presentation.assistant.ChatMessageUiState
import com.tensoscan.ui.common.components.BottomBarNavigation
import com.tensoscan.ui.common.components.TensoScanChatInputField
import com.tensoscan.ui.common.components.TensoScanSendButton
import com.tensoscan.ui.common.components.TopBar
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Assistant
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Camera
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Summary
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.User
import com.tensoscan.ui.model.TopBarModel
import com.tensoscan.ui.theme.BackgroundAssistantMessage
import com.tensoscan.ui.theme.BackgroundScreenColor
import com.tensoscan.ui.theme.BackgroundUserMessage
import com.tensoscan.ui.theme.ButtonTextColor
import com.tensoscan.ui.theme.FontValues.Font24
import com.tensoscan.ui.theme.SizeValues.Size02
import com.tensoscan.ui.theme.SizeValues.Size04
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size16
import com.tensoscan.ui.theme.TensoScanTypography
import com.tensoscan.ui.R.drawable as RDrawable
import com.tensoscan.ui.R.string as RString

@Composable
internal fun AssistantScreen(mainNavController: NavHostController, viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopBar(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                ),
            )
        },
        bottomBar = {
            BottomBarNavigation(
                items = listOf(User(), Camera(), Summary(), Assistant()),
                navController = mainNavController,
            )
        },
    ) { padding ->
        AssistantContent(state = state, onAction = viewModel::onAction, modifier = Modifier.padding(padding))
    }
}

@Composable
private fun AssistantContent(
    state: AssistantUiState,
    onAction: (AssistantUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val lastIndex = state.messages.size + (if (state.isWaitingForAnswer) 1 else 0) - 1
    LaunchedEffect(lastIndex) {
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Column(
        modifier = modifier
            .background(BackgroundScreenColor)
            .fillMaxSize()
            .imePadding()
            .padding(Size16),
    ) {
        Text(
            text = stringResource(RString.assistant_disclaimer),
            style = TensoScanTypography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = Size08),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.messages.isEmpty() && !state.isWaitingForAnswer) {
                Text(
                    text = stringResource(RString.assistant_empty_hint),
                    style = TensoScanTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(Size16),
                )
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(bottom = Size08)) {
                items(state.messages, key = { it.id }) { message -> ChatMessageBubble(message) }
                if (state.isWaitingForAnswer) {
                    item(key = "typing") { TypingBubble() }
                }
            }
        }

        state.error?.let { error ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(error.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    style = TensoScanTypography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                if (error != AssistantError.SESSION_EXPIRED) {
                    TextButton(onClick = { onAction(AssistantUiAction.SendClicked) }) {
                        Text(stringResource(RString.common_retry))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Size08),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TensoScanChatInputField(
                value = state.draft,
                onValueChange = { onAction(AssistantUiAction.DraftChanged(it)) },
                placeholder = stringResource(RString.type_a_message_text),
                modifier = Modifier.weight(1f).padding(end = Size04),
                onClear = { onAction(AssistantUiAction.DraftChanged("")) },
            )
            TensoScanSendButton(onClick = { onAction(AssistantUiAction.SendClicked) }, enabled = state.canSend)
        }
    }
}

private fun AssistantError.messageRes(): Int = when (this) {
    AssistantError.RATE_LIMITED -> RString.assistant_error_rate_limited
    AssistantError.NETWORK -> RString.assistant_error_network
    AssistantError.UNAVAILABLE -> RString.assistant_error_unavailable
    AssistantError.SESSION_EXPIRED -> RString.assistant_error_session_expired
    AssistantError.UNKNOWN -> RString.assistant_error_unknown
}

@Composable
private fun ChatMessageBubble(message: ChatMessageUiState) {
    val spoken = stringResource(
        if (message.fromUser) RString.assistant_message_from_user else RString.assistant_message_from_assistant,
        message.text,
    )
    Bubble(fromUser = message.fromUser, modifier = Modifier.semantics { contentDescription = spoken }) {
        Text(text = message.text, style = TensoScanTypography.bodySmall, color = ButtonTextColor)
    }
}

@Composable
private fun TypingBubble() {
    val description = stringResource(RString.assistant_typing_content_description)
    Bubble(fromUser = false, modifier = Modifier.semantics { contentDescription = description }) {
        AnimatedEllipsisText()
    }
}

@Composable
private fun Bubble(fromUser: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().padding(Size08),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (fromUser) BackgroundUserMessage else BackgroundAssistantMessage,
                    shape = RoundedCornerShape(Size16),
                )
                .padding(horizontal = Size16, vertical = Size08),
        ) {
            content()
        }
    }
}

@Composable
private fun AnimatedEllipsisText() {
    val dotDelays = listOf(0, 150, 300)
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val alphas =
        dotDelays.map { delay ->
            infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec =
                infiniteRepeatable(
                    animation =
                    keyframes {
                        durationMillis = 900
                        0.3f at 0
                        1f at 300
                        0.3f at 600
                    },
                    initialStartOffset = StartOffset(delay),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dotAlpha$delay",
            )
        }

    Row {
        alphas.forEachIndexed { index, alpha ->
            Text(
                text = ".",
                color = Color.White.copy(alpha = alpha.value),
                fontSize = Font24,
                modifier =
                Modifier
                    .padding(horizontal = Size02)
                    .offset(y = (-alpha.value * 4).dp),
            )
        }
    }
}
