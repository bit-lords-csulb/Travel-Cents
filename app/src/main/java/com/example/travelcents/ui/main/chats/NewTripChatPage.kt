package com.example.travelcents.ui.main.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.travelcents.ui.theme.*

data class Friend(
    val uid: String = "",
    val displayName: String = "",
    val email: String = ""
)

data class Destination(
    val name: String,
    val country: String,
    val emoji: String
)

val popularDestinations = listOf(
    Destination("Bali", "Indonesia", "🏝️"),
    Destination("Hawaii", "USA", "🌺"),
    Destination("Maldives", "South Asia", "⛱️"),
    Destination("Paris", "France", "💃"),
    Destination("Swiss Alps", "Switzerland", "🏔️"),
    Destination("Cancún", "Mexico", "🌊")
)

@Composable
fun NewTripChatPage(
    onBackClick: () -> Unit = {},
    onTripCreated: (Group) -> Unit = {},
    viewModel: NewTripViewModel = viewModel()
) {
    val selectedFriends by viewModel.selectedFriends.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val friendSearch by viewModel.friendSearch.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val filteredFriends = viewModel.filteredFriends.collectAsState().value


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
                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeepSea3)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepSea5, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text("New Trip Chat", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Search bar
            item {
                TextField(
                    value = friendSearch,
                    onValueChange = { viewModel.onSearchChange(it) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(48.dp)),
                    placeholder = { Text("Search friends...", color = DeepSea5.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DeepSea5.copy(alpha = 0.7f)) },
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

            // Search results
            if (filteredFriends.isNotEmpty()) {
                items(filteredFriends.take(5)) { friend ->
                    val alreadySelected = selectedFriends.any { it.uid == friend.uid }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepSea2)
                            .clickable { if (!alreadySelected) viewModel.selectFriend(friend) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(DeepSea3),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(friend.displayName.take(2).uppercase(), color = DeepSea5, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(friend.displayName, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(friend.email, color = DeepSea5.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Selected friend chips
            if (selectedFriends.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedFriends) { friend ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(48.dp))
                                    .background(DeepSea3)
                                    .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(22.dp).clip(CircleShape).background(DeepSea2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(friend.displayName.take(1).uppercase(), color = DeepSea5, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(friend.displayName.split(" ").first(), color = DeepSea5, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = DeepSea5.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp).clickable { viewModel.removeFriend(friend) }
                                )
                            }
                        }
                    }
                }
            }

            // Destination header
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Choose Destination", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(Optional)", color = DeepSea5.copy(alpha = 0.45f), fontSize = 13.sp)
                }
            }

            // Destination grid
            items(popularDestinations.chunked(2)) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { dest ->
                        val isSelected = selectedDestination == dest
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DeepSea2)
                                .then(if (isSelected) Modifier.border(2.dp, DeepSea4, RoundedCornerShape(16.dp)) else Modifier)
                                .clickable { viewModel.toggleDestination(dest) }
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(dest.emoji, fontSize = 30.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(dest.name, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(dest.country, color = DeepSea5.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Start Planning button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { viewModel.createTrip { onTripCreated(it) } },
                    enabled = selectedFriends.isNotEmpty() && !isCreating,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepSea3,
                        disabledContainerColor = DeepSea3.copy(alpha = 0.4f)
                    )
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(color = DeepSea5, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Start Planning ✈", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}