package com.example.travelcents.ui.main.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private val ChatBlue = Color(0xFF64B5F6)

@Composable
fun AiTripChatPage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val messages = viewModel.messages
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF010E24))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChatBlue)
                    }
                    Column {
                        Text("AI Travel Assistant", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                        Text(
                            "Describe your dream trip in your own words",
                            fontSize = 11.sp,
                            color = DeepSea5.copy(alpha = 0.5f)
                        )
                    }
                }
                // Right side: small AI icon badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ChatBlue.copy(alpha = 0.12f), CircleShape)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = ChatBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // Accent line at bottom of header (same as wizard steps)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(ChatBlue, ChatBlue.copy(alpha = 0.4f))
                        )
                    )
            )
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp)
        ) {
            items(messages) { message ->
                AiChatBubble(text = message.text, isFromUser = message.isFromUser)
            }

            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea2)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp)),
                placeholder = {
                    Text("Ask about destinations, budgets, itineraries...", color = DeepSea5.copy(alpha = 0.4f))
                },
                singleLine = false,
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DeepSea3,
                    unfocusedContainerColor = DeepSea3,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = DeepSea5,
                    focusedTextColor = DeepSea5,
                    unfocusedTextColor = DeepSea5
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isBlank() || isLoading) DeepSea3.copy(alpha = 0.5f) else DeepSea3)
                    .clickable(enabled = inputText.isNotBlank() && !isLoading) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isBlank() || isLoading) DeepSea5.copy(alpha = 0.4f) else DeepSea5,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AiChatBubble(text: String, isFromUser: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
    ) {
        if (!isFromUser) {
            Text(
                text = "AI Assistant",
                color = DeepSea5.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isFromUser) 18.dp else 4.dp,
                        bottomEnd = if (isFromUser) 4.dp else 18.dp
                    )
                )
                .background(if (isFromUser) DeepSea3 else DeepSea2)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = text, color = DeepSea5, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = DeepSea4,
            strokeWidth = 2.dp
        )
        Text(
            text = "AI is thinking...",
            color = DeepSea4,
            fontSize = 12.sp
        )
    }
}
