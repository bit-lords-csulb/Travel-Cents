package com.example.travelcents.ui.main.chats

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null
)

fun formatMessageTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
}

@Composable
fun ChatPage(
    group: Group,
    onBackClick: () -> Unit = {}
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUid = auth.currentUser?.uid ?: ""
    val currentName = auth.currentUser?.displayName ?: "Me"

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Real-time messages listener
    DisposableEffect(group.id) {
        val listener = db.collection("groups")
            .document(group.id)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val text = messageText.trim()
        if (text.isEmpty()) return

        val message = hashMapOf(
            "text" to text,
            "senderId" to currentUid,
            "senderName" to currentName,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val groupRef = db.collection("groups").document(group.id)
        db.runBatch { batch ->
            batch.set(groupRef.collection("messages").document(), message)
            batch.update(groupRef, mapOf(
                "lastMessage" to text,
                "lastMessageTime" to FieldValue.serverTimestamp()
            ))
        }
        messageText = ""
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
                .padding(top = 48.dp, bottom = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepSea5)
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DeepSea3),
                    contentAlignment = Alignment.Center
                ) {
                    if (group.groupImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = group.groupImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = group.name.take(2).uppercase(),
                            color = DeepSea5,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        color = DeepSea5,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${group.members.size} members",
                        color = DeepSea5.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepSea5),
                    border = BorderStroke(1.dp, DeepSea5.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("EVENTS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = DeepSea5)
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
            items(messages, key = { it.id }) { message ->
                val isMine = message.senderId == currentUid
                ChatBubble(message = message, isMine = isMine)
            }
        }

        // Message Text Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea2)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp)),
                placeholder = {
                    Text("Type a message...", color = DeepSea5.copy(alpha = 0.4f))
                },
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
                    .clickable { sendMessage() },
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
fun ChatBubble(message: Message, isMine: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                text = message.senderName,
                color = DeepSea5.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

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
            Text(
                text = message.text,
                color = DeepSea5,
                fontSize = 14.sp
            )
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