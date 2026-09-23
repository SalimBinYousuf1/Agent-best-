package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistant.voice.SpeechState
import com.example.ui.components.AssistantStatusBar
import com.example.ui.components.ChatComposer
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ExamplePromptChip
import com.example.ui.components.TypingIndicator
import com.example.ui.theme.BackgroundWhite
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.Dimens
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun ChatScreen(
    viewModel: AssistantViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSetup: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message or loading
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            val target = if (isLoading) messages.size else messages.size - 1
            listState.animateScrollToItem(target.coerceAtLeast(0))
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_screen"),
        containerColor = BackgroundWhite,
        topBar = {
            AssistantStatusBar(
                modelName = selectedModel,
                isOnline = isOnline,
                onNewChatClick = {
                    viewModel.startNewConversation()
                    textInput = ""
                },
                onHistoryClick = onNavigateToHistory,
                onSettingsClick = onNavigateToSettings,
                onStatusClick = onNavigateToDiagnostics
            )
        },
        bottomBar = {
            ChatComposer(
                inputText = textInput,
                onInputChange = { textInput = it },
                onSendMessage = { prompt ->
                    viewModel.sendUserPrompt(prompt)
                    textInput = ""
                },
                onMicClick = {
                    if (speechState is SpeechState.Listening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                isListening = speechState is SpeechState.Listening,
                isLoading = isLoading
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Dimens.MaxContentWidth),
                contentPadding = PaddingValues(
                    top = Dimens.Spacing8,
                    bottom = Dimens.Spacing16
                )
            ) {
                if (messages.isEmpty()) {
                    item {
                        ChatEmptyState(
                            onSuggestionClick = { prompt ->
                                viewModel.sendUserPrompt(prompt)
                            }
                        )
                    }
                } else {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            onRetryClick = {
                                viewModel.retryLastRequest()
                            }
                        )
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing4),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TypingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatEmptyState(
    isAutonomous: Boolean = true,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Spacing24, vertical = Dimens.Spacing32)
            .testTag("chat_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Clean restrained Assistant avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(BrandAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing16))

        Text(
            text = "What can I help with today?",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimens.Spacing8))

        Text(
            text = "Salim Assistant can schedule alarms, create calendar events, send direct messages, place phone calls, and answer your questions.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.Spacing8)
        )

        Spacer(modifier = Modifier.height(Dimens.Spacing32))

        // 3-5 Example Prompt Chips
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing10)
        ) {
            ExamplePromptChip(
                text = "Set an alarm for 7 AM",
                icon = Icons.Default.Alarm,
                onClick = { onSuggestionClick("Set an alarm for 7:00 AM") },
                modifier = Modifier.fillMaxWidth()
            )

            ExamplePromptChip(
                text = "Schedule a meeting tomorrow",
                icon = Icons.Default.CalendarToday,
                onClick = { onSuggestionClick("Schedule a meeting tomorrow at 2 PM called Team Sync") },
                modifier = Modifier.fillMaxWidth()
            )

            ExamplePromptChip(
                text = "Open YouTube",
                icon = Icons.Default.Launch,
                onClick = { onSuggestionClick("Open YouTube") },
                modifier = Modifier.fillMaxWidth()
            )

            ExamplePromptChip(
                text = "Explain Android permissions",
                icon = Icons.Default.Security,
                onClick = { onSuggestionClick("Explain Android assistant permissions and why they are needed") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
