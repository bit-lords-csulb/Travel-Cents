package com.example.travelcents.ui.main.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val groupImageUrl: String = ""
)

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = timestamp.toDate() }
    return when {
        now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
        now.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1 -> "Yesterday"
        now.get(Calendar.WEEK_OF_YEAR) == msgCal.get(Calendar.WEEK_OF_YEAR) ->
            SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp.toDate())
        else -> "Last week"
    }
}

@Composable
fun ChatsPage(
    modifier: Modifier = Modifier,
    viewModel: ChatsViewModel = viewModel(),
    onNewChatClick: () -> Unit = {},
    onGroupClick: (Group) -> Unit = {}
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredGroups by viewModel.filteredGroups.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chats",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSea5
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DeepSea3)
                            .clickable { onNewChatClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = DeepSea5)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(48.dp)),
                    placeholder = {
                        Text("Search conversations...", color = DeepSea5.copy(alpha = 0.5f))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = DeepSea5.copy(alpha = 0.7f))
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
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) "No chats yet" else "No results found",
                    color = DeepSea5.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredGroups, key = { it.id }) { group ->
                    GroupChatRow(group = group, onClick = { onGroupClick(group) })
                }
            }
        }
    }
}

@Composable
fun GroupChatRow(group: Group, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepSea2)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(DeepSea3),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = group.name.take(2).uppercase(),
                color = DeepSea5,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                color = DeepSea5,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = group.lastMessage,
                color = DeepSea5.copy(alpha = 0.55f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatTimestamp(group.lastMessageTime),
            color = DeepSea5.copy(alpha = 0.4f),
            fontSize = 11.sp
        )
    }
}

// Nav Wrapper
@Composable
fun ChatsScreen(modifier: Modifier = Modifier) {
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var showNewTrip by remember { mutableStateOf(false) }

    when {
        selectedGroup != null -> {
            ChatPage(
                group = selectedGroup!!,
                onBackClick = { selectedGroup = null }
            )
        }
        showNewTrip -> {
            NewTripChatPage(
                onBackClick = { showNewTrip = false },
                onTripCreated = { newGroup ->
                    showNewTrip = false
                    selectedGroup = newGroup
                }
            )
        }
        else -> {
            ChatsPage(
                modifier = modifier,
                onNewChatClick = { showNewTrip = true },
                onGroupClick = { group -> selectedGroup = group }
            )
        }
    }
}