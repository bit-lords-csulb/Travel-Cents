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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State for Deletion
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    // State for Bottom Sheet (Full Details)
    var selectedEventForDetails by remember { mutableStateOf<Event?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    // 1. Full Details Bottom Sheet
    if (showSheet && selectedEventForDetails != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = DeepSea2,
            dragHandle = { BottomSheetDefaults.DragHandle(color = DeepSea5.copy(alpha = 0.3f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 64.dp)
            ) {
                Text(selectedEventForDetails!!.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = DeepSea5.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                    Text(selectedEventForDetails!!.location, color = DeepSea5.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = selectedEventForDetails!!.description,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = DeepSea5.copy(alpha = 0.8f)
                )
            }
        }
    }

    // 2. Delete Confirmation Dialog
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            containerColor = DeepSea2,
            title = { Text("Delete Event", color = DeepSea5, fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${event.title}\"?", color = DeepSea5.copy(alpha = 0.7f)) },
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

    Column(modifier = Modifier.fillMaxSize().background(DeepSea1)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .padding(top = 48.dp, bottom = 20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${group.name} Trip Ideas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                Text("PROPOSED EVENTS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DeepSea5.copy(alpha = 0.5f), letterSpacing = 1.sp)
            }

            IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 8.dp).size(48.dp).align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepSea5, modifier = Modifier.size(28.dp))
            }

            IconButton(onClick = onNewEvent, modifier = Modifier.padding(end = 12.dp).size(48.dp).clip(CircleShape).background(DeepSea3).align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = DeepSea5, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepSea5)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        currentUid = viewModel.currentUid,
                        onClick = {
                            selectedEventForDetails = event
                            showSheet = true
                        },
                        onUpvote = { viewModel.upvote(event) },
                        onDownvote = { viewModel.downvote(event) },
                        onCommentClick = { onEventClick(event) },
                        onLongPress = {
                            eventToDelete = event.takeIf { it.createdBy == viewModel.currentUid }
                        }
                    )
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
    onClick: () -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onCommentClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val score = event.upvotes.size - event.downvotes.size
    val hasUpvoted = event.upvotes.contains(currentUid)
    val hasDownvoted = event.downvotes.contains(currentUid)

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = DeepSea2),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                if (event.photoUrl.isNotEmpty()) {
                    AsyncImage(model = event.photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(DeepSea3))
                }
                UserOverlayTag(event.createdByName)
            }

            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                VotingSideBar(score, hasUpvoted, hasDownvoted, onUpvote, onDownvote)
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = event.title, color = DeepSea5, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    if (event.description.isNotBlank()) {
                        Text(text = event.description, color = DeepSea5.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2, lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // FIXED ALIGNMENT BOX
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = DeepSea5.copy(alpha = 0.4f), modifier = Modifier.size(13.dp))
                                Text(text = event.location, color = DeepSea5.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null, tint = DeepSea5.copy(alpha = 0.4f), modifier = Modifier.size(13.dp))
                                Text(text = event.time, color = DeepSea5.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }

                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).clip(RoundedCornerShape(10.dp)).clickable { onCommentClick() },
                            color = DeepSea3.copy(alpha = 0.6f)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = DeepSea5, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "${event.commentCount}", color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserOverlayTag(name: String) {
    Row(
        modifier = Modifier.padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.Gray))
        Spacer(modifier = Modifier.width(8.dp))
        Text(name.split(" ").first(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VotingSideBar(score: Int, upvoted: Boolean, downvoted: Boolean, onUp: () -> Unit, onDown: () -> Unit) {
    Column(
        modifier = Modifier.width(40.dp).clip(RoundedCornerShape(12.dp)).background(DeepSea3).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onUp() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = if (upvoted) Color(0xFF81C784) else DeepSea5.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
        Text(text = "$score", color = DeepSea5, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 2.dp))
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onDown() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = if (downvoted) Color(0xFFE57373) else DeepSea5.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}