package com.example.ui.feature.chatbot

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.domain.feature.chatbot.model.ChatMessageModel
import com.example.ui.common.components.BottomBarNavigation
import com.example.ui.common.components.TopBarView
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.Camera
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.Chatbot
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.Summary
import com.example.ui.common.navigation.bottomnavigation.BottomBarItem.User
import com.example.ui.model.TopBarModel
import com.example.ui.theme.BackgroundAssistantMessage
import com.example.ui.theme.BackgroundScreenColor
import com.example.ui.theme.BackgroundUserMessage
import com.example.ui.theme.Fontalues.Font24
import com.example.ui.theme.SizeValues.Size02
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size12
import com.example.ui.theme.SizeValues.Size16
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import com.example.ui.R.drawable as RDrawable
import com.example.ui.R.string as RString

@OptIn(KoinExperimentalAPI::class)
@Composable
fun ChatbotScreenView(
    mainNavController: NavHostController
) {
    val chatbotViewModel = koinViewModel<ChatbotViewModel>()
    val chatbotState by chatbotViewModel.state.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBarView(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                    icon = Icons.Default.Settings
                )
            )
        },
        bottomBar = {
            val items = listOf(User(), Camera(), Summary(), Chatbot())
            BottomBarNavigation(items = items, navController = mainNavController)
        }
    ) { padding ->
        ChatbotScreenContent(
            padding = padding,
            chatbotState = chatbotState,
            prompt = prompt,
            onPromptChange = { prompt = it },
            onSendPrompt = {
                chatbotViewModel.sendMessage(prompt)
                prompt = ""
            }
        )
    }
}

@Composable
fun ChatbotScreenContent(
    padding: PaddingValues,
    chatbotState: ChatBoState,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(BackgroundScreenColor)
            .fillMaxSize()
            .padding(padding)
            .padding(Size16)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = Size08)
            ) {
                items(chatbotState.history) { message ->
                    ChatMessageBubble(message = message)
                }
            }

            if (chatbotState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        ChatInputField(
            prompt = prompt,
            onPromptChange = onPromptChange,
            onSendPrompt = onSendPrompt
        )

        chatbotState.error?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier
                    .padding(Size08)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessageModel) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Arrangement.End else Arrangement.Start
    val backgroundColor = if (isUser) BackgroundUserMessage else BackgroundAssistantMessage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Size08),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape = RoundedCornerShape(Size16))
                .padding(horizontal = Size16, vertical = Size08)
        ) {
            if (message.conntent == "...") {
                AnimatedEllipsisText()
            } else {
                Text(
                    text = message.conntent,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ChatInputField(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Size08),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = Size08),
            placeholder = { Text(text = "Escribe un mensaje ...") },
            shape = RoundedCornerShape(Size16),
            singleLine = true,
            trailingIcon = {
                if (prompt.isNotBlank()) {
                    IconButton(onClick = { onPromptChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear text"
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        ElevatedButton(
            onClick = onSendPrompt,
            shape = RoundedCornerShape(Size12),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send"
            )
        }
    }
}

@Composable
fun AnimatedEllipsisText() {
    val dotDelays = listOf(0, 150, 300)
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val alphas = dotDelays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0.3f at 0
                    1f at 300
                    0.3f at 600
                },
                initialStartOffset = StartOffset(delay),
                repeatMode = RepeatMode.Restart
            ),
            label = "dotAlpha$delay"
        )
    }

    Row {
        alphas.forEachIndexed { index, alpha ->
            Text(
                text = ".",
                color = Color.White.copy(alpha = alpha.value),
                fontSize = Font24,
                modifier = Modifier
                    .padding(horizontal = Size02)
                    .offset(y = (-alpha.value * 4).dp)
            )
        }
    }
}