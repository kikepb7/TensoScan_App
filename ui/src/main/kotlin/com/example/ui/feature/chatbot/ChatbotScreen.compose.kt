package com.example.ui.feature.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size16
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun ChatbotScreenView() {
    val chatbotViewModel = koinViewModel<ChatbotViewModel>()
    val chatbotState by chatbotViewModel.state.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Size16)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = Size08)
        ) {
            items(chatbotState.history) { message ->
                if (message.role == "user") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Size08),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Green, shape = RoundedCornerShape(Size16))
                                .padding(horizontal = Size16, vertical = Size08)
                        ) {
                            Text(
                                text = message.conntent,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Size08),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Blue, shape = RoundedCornerShape(Size16))
                                .padding(horizontal = Size16, vertical = Size08)
                        ) {
                            Text(
                                text = message.conntent,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Size08),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Size08),
                placeholder = { Text("Type a message...") }
            )
            Button(
                onClick = {
                    chatbotViewModel.sendMessage(prompt = prompt)
                    prompt = ""
                }
            ) {
                Text("Send")
            }
        }

        if (chatbotState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Size08)
            )
        }

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