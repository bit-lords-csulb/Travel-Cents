package com.example.travelcents.ui.main.chats.chat

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.social.model.Message
import com.example.travelcents.ui.components.ProfileAvatar
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea5
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatMessageTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val now    = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = timestamp.toDate() }
    val time   = SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
    return when {
        // Same day — just show time
        now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) &&
                now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> time

        // Yesterday
        now.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1 &&
                now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> "Yesterday $time"

        // Same week — show day name
        now.get(Calendar.WEEK_OF_YEAR) == msgCal.get(Calendar.WEEK_OF_YEAR) &&
                now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) ->
            SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(timestamp.toDate())

        // Same year — show month/day
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) ->
            SimpleDateFormat("M/d h:mm a", Locale.getDefault()).format(timestamp.toDate())

        // Different year — show full date
        else -> SimpleDateFormat("M/d/yy h:mm a", Locale.getDefault()).format(timestamp.toDate())
    }
}

@Composable
fun ChatPage(
    group: Group,
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(
        key = group.id,
        factory = ChatViewModel.Factory(group)
    ),
    onEventsClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onTripCardClick: ((tripId: String, ownerUid: String) -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUid = viewModel.currentUid
    val listState = rememberLazyListState()
    val liveGroup by viewModel.groupState.collectAsState()
    val senderProfiles by viewModel.senderProfiles.collectAsState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
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
                .padding(top = 8.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, modifier = Modifier.size(32.dp), contentDescription = "Back", tint = DeepSea5)
                }

                // Group Chat Image - Fixed to use liveGroup
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DeepSea3),
                    contentAlignment = Alignment.Center
                ) {
                    // Priority: Live Data -> Static Initial Data
                    val url = liveGroup?.groupImageUrl ?: group.groupImageUrl
                    val currentName = liveGroup?.name ?: group.name

                    if (url.startsWith("http")) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (url.isNotEmpty() && url.length <= 4) {
                        Text(url, fontSize = 20.sp)
                    } else {
                        Text(
                            text = currentName.take(2).uppercase(),
                            color = DeepSea5,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Title - Fixed to use liveGroup
                    Text(
                        text = liveGroup?.name ?: group.name,
                        color = DeepSea5,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    // Member Count - Fixed to use liveGroup
                    Text(
                        text = "${liveGroup?.members?.size ?: group.members.size} members",
                        color = DeepSea5.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                // Event Button
                OutlinedButton(
                    onClick = { onEventsClick() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepSea5),
                    border = BorderStroke(1.dp, DeepSea5.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("EVENTS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Edit Chat Button Dots
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Edit Chat", tint = DeepSea5)
                }
            }
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
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                val previousMessage = messages.getOrNull(index - 1)
                val senderProfile = senderProfiles[message.senderId]
                ChatBubble(
                    message = message,
                    isMine = message.senderId == currentUid,
                    showSenderHeader = previousMessage?.senderId != message.senderId,
                    senderDisplayName = senderProfile?.displayName
                        ?.takeIf(String::isNotBlank)
                        ?: message.senderName,
                    senderPhotoUrl = senderProfile?.photoUrl.orEmpty(),
                    onTripCardClick = onTripCardClick
                )
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
                value = messageText,
                onValueChange = { viewModel.onMessageTextChange(it) },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(28.dp)),
                placeholder = { Text("Type a message...", color = DeepSea5.copy(alpha = 0.4f)) },
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
                    .clickable { viewModel.sendMessage() },
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

@Composable
fun ChatBubble(
    message: Message,
    isMine: Boolean,
    showSenderHeader: Boolean,
    senderDisplayName: String,
    senderPhotoUrl: String,
    onTripCardClick: ((tripId: String, ownerUid: String) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (showSenderHeader) {
            ChatSenderHeader(
                displayName = senderDisplayName,
                photoUrl = senderPhotoUrl,
                isMine = isMine
            )
        }

        if (message.messageType == "trip_card") {
            TripCardBubble(
                message = message,
                isMine = isMine,
                onClick = {
                    val tripId = message.sharedTripId
                    val ownerUid = message.ownerUid
                    if (tripId != null && ownerUid != null) {
                        onTripCardClick?.invoke(tripId, ownerUid)
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMine) 18.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 18.dp
                        )
                    )
                    .background(if (isMine) DeepSea3 else DeepSea2)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(text = message.text, color = DeepSea5, fontSize = 14.sp)
            }
        }

        Text(
            text = formatMessageTime(message.timestamp),
            color = DeepSea5.copy(alpha = 0.35f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp, end = 4.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ChatSenderHeader(
    displayName: String,
    photoUrl: String,
    isMine: Boolean
) {
    val resolvedName = displayName.ifBlank { if (isMine) "You" else "Unknown" }
    Row(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .padding(
                start = if (isMine) 0.dp else 4.dp,
                end = if (isMine) 4.dp else 0.dp,
                bottom = 2.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isMine) {
            ProfileAvatar(
                photoUrl = photoUrl,
                contentDescription = "$resolvedName profile picture",
                modifier = Modifier.size(24.dp),
                borderColor = DeepSea3,
                backgroundColor = DeepSea3,
                placeholderTint = DeepSea5,
                borderWidth = 1.dp,
                iconSize = 13.dp
            )
        }
        Text(
            text = resolvedName,
            color = DeepSea5.copy(alpha = 0.62f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (isMine) {
            ProfileAvatar(
                photoUrl = photoUrl,
                contentDescription = "$resolvedName profile picture",
                modifier = Modifier.size(24.dp),
                borderColor = DeepSea3,
                backgroundColor = DeepSea3,
                placeholderTint = DeepSea5,
                borderWidth = 1.dp,
                iconSize = 13.dp
            )
        }
    }
}

@Composable
private fun TripCardBubble(
    message: Message,
    isMine: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isMine) DeepSea3 else DeepSea2
    Card(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 280.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(
            topStart = 18.dp, topEnd = 18.dp,
            bottomStart = if (isMine) 18.dp else 4.dp,
            bottomEnd = if (isMine) 4.dp else 18.dp
        ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, DeepSea5.copy(alpha = 0.1f))
    ) {
        Column {
            // Cover image or placeholder
            val cover = message.coverImageUrl ?: ""
            if (cover.isNotBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF0B203D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✈", fontSize = 28.sp)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                // "Shared a trip" label
                Text(
                    text = "📍 Shared a trip",
                    color = DeepSea5.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.tripName ?: "Trip",
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!message.tripDestination.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(message.tripDestination)
                            val from = message.tripDateFrom
                            val to = message.tripDateTo
                            if (!from.isNullOrBlank()) append("  •  $from")
                            if (!to.isNullOrBlank()) append(" → $to")
                        },
                        color = DeepSea5.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to view trip →",
                    color = Color(0xFF64B5F6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
