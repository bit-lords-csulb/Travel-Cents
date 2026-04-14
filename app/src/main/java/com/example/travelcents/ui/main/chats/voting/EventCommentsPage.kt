package com.example.travelcents.ui.main.chats.voting

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.model.Event
import com.example.travelcents.data.model.EventComment
import com.example.travelcents.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventCommentsPage(
    event: Event,
    groupId: String,
    onBackClick: () -> Unit = {},
    viewModel: EventCommentsViewModel = viewModel(
        key = "${groupId}_${event.id}",
        factory = EventCommentsViewModel.Factory(groupId, event.id)
    )
) {
    val comments by viewModel.comments.collectAsState()
    val commentText by viewModel.commentText.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) listState.animateScrollToItem(comments.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            // Back button
            IconButton(
                onClick = {
                    onBackClick()
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    modifier = Modifier.size(32.dp),
                    contentDescription = "Back",
                    tint = DeepSea5
                )
            }

            // Comments Title
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Comments",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepSea5
                )
                Text(
                    text = event.title.uppercase(),
                    fontSize = 10.sp,
                    color = DeepSea5.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }

        // Comments list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp)
        ) {
            items(comments, key = { it.id }) { comment ->
                CommentBubble(comment = comment, isMe = comment.senderId == viewModel.currentUid)
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea2)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = commentText,
                onValueChange = { viewModel.onCommentTextChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp)),
                placeholder = { Text("Add a comment...", color = DeepSea5.copy(alpha = 0.4f)) },
                singleLine = true,
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
                    .background(DeepSea3)
                    .clickable { viewModel.sendComment() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = DeepSea5,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Helpers
@Composable
fun CommentBubble(comment: EventComment, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeepSea3),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.senderName.take(2).uppercase(),
                    color = DeepSea5,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (!isMe) {
                Text(
                    text = comment.senderName,
                    color = DeepSea5.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isMe) 18.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 18.dp
                        )
                    )
                    .background(if (isMe) DeepSea3 else DeepSea2)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(text = comment.text, color = DeepSea5, fontSize = 14.sp)
            }
            comment.timestamp?.let {
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(it.toDate()),
                    color = DeepSea5.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                )
            }
        }
    }
}
