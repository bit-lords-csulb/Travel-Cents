package com.example.travelcents.ui.main.chats.groups

import androidx.activity.compose.BackHandler // <--- NEW IMPORT
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.example.travelcents.data.model.Group
import com.example.travelcents.ui.theme.*

@Composable
fun NewTripChatPage(
    onBackClick: () -> Unit = {},
    onTripCreated: (Group) -> Unit = {},
    viewModel: NewTripViewModel = viewModel()
) {
    val selectedFriends by viewModel.selectedFriends.collectAsState()
    val friendSearch by viewModel.friendSearch.collectAsState()
    val chatName by viewModel.chatName.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedTrip by viewModel.selectedTrip.collectAsState()
    val userTrips by viewModel.userTrips.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val filteredFriends = viewModel.filteredFriends.collectAsState().value

    BackHandler {
        viewModel.resetForm()
        onBackClick()
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepSea1)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 20.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {

                IconButton(onClick = {
                    viewModel.resetForm()
                    onBackClick()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepSea5)
                }
                Text("New Trip Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Chat Name & Description Inputs
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    LabeledTextField(
                        label = "Chat Name",
                        value = chatName,
                        onValueChange = { viewModel.onChatNameChange(it) },
                        placeholder = "e.g. Bali Squad 2024"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LabeledTextField(
                        label = "Description",
                        value = description,
                        onValueChange = { viewModel.onDescriptionChange(it) },
                        placeholder = "Planning our epic summer trip!",
                        singleLine = false,
                        modifier = Modifier.height(100.dp)
                    )
                }
            }

            // Friend Search
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("Add Friends", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = friendSearch,
                        onValueChange = { viewModel.onSearchChange(it) },
                        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(48.dp)),
                        placeholder = { Text("Search friends...", color = DeepSea5.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = DeepSea5.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = DeepSea5, focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5
                        )
                    )
                }
            }

            // Search results
            if (filteredFriends.isNotEmpty()) {
                items(filteredFriends.take(5), span = { GridItemSpan(maxLineSpan) }) { friend ->
                    val alreadySelected = selectedFriends.any { it.uid == friend.uid }
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DeepSea2)
                            .clickable { if (!alreadySelected) viewModel.selectFriend(friend) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(DeepSea3), contentAlignment = Alignment.Center) {
                            Text(friend.displayName.take(2).uppercase(), color = DeepSea5, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(friend.displayName, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Selected friend chips
            if (selectedFriends.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lazyRowItems(selectedFriends) { friend ->
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(48.dp)).background(DeepSea3).padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(DeepSea2), contentAlignment = Alignment.Center) {
                                    Text(friend.displayName.take(1).uppercase(), color = DeepSea5, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(friend.displayName.split(" ").first(), color = DeepSea5, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Close, null, tint = DeepSea5.copy(alpha = 0.6f), modifier = Modifier.size(16.dp).clickable { viewModel.removeFriend(friend) })
                            }
                        }
                    }
                }
            }

            // Link to Existing Trip Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Link to Existing Trip", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(Optional)", color = DeepSea5.copy(alpha = 0.45f), fontSize = 13.sp)
                }
            }

            // Dynamic Trips Grid
            if (userTrips.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("No trips found. Create a trip first to link it!", color = DeepSea5.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                items(userTrips) { trip ->
                    val isSelected = selectedTrip == trip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DeepSea2)
                            .then(if (isSelected) Modifier.border(2.dp, DeepSea4, RoundedCornerShape(16.dp)) else Modifier)
                            .clickable { viewModel.toggleTripSelection(trip) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(trip.emoji, fontSize = 30.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(trip.tripName, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                            Text(trip.destination, color = DeepSea5.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Start Planning button
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    // --- 3. ON SUCCESSFUL CREATION ---
                    onClick = {
                        viewModel.createTrip { group ->
                            viewModel.resetForm()
                            onTripCreated(group)
                        }
                    },
                    enabled = selectedFriends.isNotEmpty() && !isCreating,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSea3, disabledContainerColor = DeepSea3.copy(alpha = 0.4f))
                ) {
                    if (isCreating) CircularProgressIndicator(color = DeepSea5, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Start Planning ✈", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

// Helper Text Field
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(label, color = DeepSea5, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = DeepSea5.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            singleLine = singleLine,
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
