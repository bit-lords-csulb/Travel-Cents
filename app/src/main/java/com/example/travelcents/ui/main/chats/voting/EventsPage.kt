package com.example.travelcents.ui.main.chats.voting

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelcents.data.model.Event
import com.example.travelcents.data.model.Group
import com.example.travelcents.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventsPage(
    group: Group,
    onBackClick: () -> Unit = {},
    onNewEvent: () -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    viewModel: EventsViewModel = viewModel(
        key = group.id,
        factory = EventsViewModel.Factory(group.id)
    )
) {
    val events       by viewModel.events.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            containerColor   = DeepSea2,
            title  = { Text("Delete Event", color = DeepSea5, fontWeight = FontWeight.Bold) },
            text   = { Text("Delete \"${event.title}\"?", color = DeepSea5.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEvent(event); eventToDelete = null }) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancel", color = DeepSea5.copy(alpha = 0.6f))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DeepSea3)
                                .clickable { onBackClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DeepSea5,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Proposed Events", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                            Text(group.name, fontSize = 12.sp, color = DeepSea5.copy(alpha = 0.5f))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DeepSea3)
                            .clickable { onNewEvent() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event", tint = DeepSea5)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DeepSea5)
                }
            }
            events.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No events yet", color = DeepSea5.copy(alpha = 0.4f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to propose one!", color = DeepSea5.copy(alpha = 0.3f), fontSize = 12.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(
                            event      = event,
                            currentUid = viewModel.currentUid,
                            onUpvote   = { viewModel.upvote(event) },
                            onDownvote = { viewModel.downvote(event) },
                            onClick    = { onEventClick(event) },
                            onLongPress = {
                                if (event.createdBy == viewModel.currentUid) eventToDelete = event
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(
    event: Event,
    currentUid: String,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val hasUpvoted   = event.upvotes.contains(currentUid)
    val hasDownvoted = event.downvotes.contains(currentUid)
    val score        = event.upvotes.size - event.downvotes.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepSea2)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        // Photo background if available
        if (event.photoUrl.isNotEmpty()) {
            AsyncImage(
                model = event.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
        }

        Column {
            if (event.photoUrl.isNotEmpty()) Spacer(modifier = Modifier.height(130.dp))

            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                // Vote column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Upvote",
                        tint = if (hasUpvoted) Color(0xFF4CAF50) else DeepSea5.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp).clickable { onUpvote() }
                    )
                    Text(
                        text = "$score",
                        color = when {
                            score > 0 -> Color(0xFF4CAF50)
                            score < 0 -> Color(0xFFE53935)
                            else      -> DeepSea5.copy(alpha = 0.5f)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Downvote",
                        tint = if (hasDownvoted) Color(0xFFE53935) else DeepSea5.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp).clickable { onDownvote() }
                    )
                }

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.title, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (event.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(event.description, color = DeepSea5.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 2)
                    }
                    if (event.location.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = DeepSea5.copy(alpha = 0.5f), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(event.location, color = DeepSea5.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                    if (event.time.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = DeepSea5.copy(alpha = 0.5f), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(event.time, color = DeepSea5.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("by ${event.createdByName}", color = DeepSea5.copy(alpha = 0.35f), fontSize = 11.sp)
                        Text("💬 ${event.commentCount}", color = DeepSea5.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}