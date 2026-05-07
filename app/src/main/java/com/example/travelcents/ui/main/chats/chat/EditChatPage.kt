package com.example.travelcents.ui.main.chats.chat

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.flow.combine
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.ui.components.ProfileAvatar
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
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChatPage(
    group: Group,
    onBackClick: () -> Unit,
    onNavigateToChats: () -> Unit,
    viewModel: EditChatViewModel = viewModel(
        key = group.id,
        factory = EditChatViewModel.Factory(group)
    )
) {
    val isOwner = viewModel.isOwner

    // State Collection
    val chatName by viewModel.chatName.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val members by viewModel.members.collectAsState()
    val memberProfiles by viewModel.memberProfiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableTrips by viewModel.availableTrips.collectAsState()
    val selectedTrip by viewModel.selectedTrip.collectAsState()

    // Member Search & Staging
    val friendSearch by viewModel.searchQuery.collectAsState()
    val filteredFriends by viewModel.filteredFriends.collectAsState()
    val stagedFriends by viewModel.stagedFriends.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentUid = Firebase.auth.currentUser?.uid ?: ""

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.updateImageUri(uri)
    }

    BackHandler { onBackClick() }

    Column(modifier = Modifier.fillMaxSize().background(DeepSea1)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, modifier = Modifier.size(32.dp), contentDescription = "Back", tint = DeepSea5)
                }
                Text("Edit Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
            }
        }

        // Reorder the list so the current user is always first
        val sortedMembers = remember(members, currentUid) {
            val others = members.filter { it != currentUid }
            if (members.contains(currentUid)) {
                listOf(currentUid) + others
            } else {
                others
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            // Group Image
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(100.dp)) {
                        Box(
                            modifier = Modifier
                                .size(100.dp).clip(CircleShape).background(DeepSea3)
                                .then(if (isOwner) Modifier.clickable { imagePickerLauncher.launch("image/*") } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(model = selectedImageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else if (group.groupImageUrl.startsWith("http")) {
                                AsyncImage(model = group.groupImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else if (group.groupImageUrl.isNotEmpty() && group.groupImageUrl.length <= 4) {
                                Text(group.groupImageUrl, fontSize = 40.sp)
                            } else {
                                Text(text = chatName.take(2).uppercase(), color = DeepSea5, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (isOwner) {
                            Box(
                                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(DeepSea4).clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.CameraAlt, contentDescription = "Edit", tint = DeepSea1, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }

            // Input Fields
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (isOwner) {
                    Column {
                        LabeledTextField(label = "Chat Name", value = chatName, onValueChange = viewModel::updateName, placeholder = "e.g. Bali Trip")
                        Spacer(modifier = Modifier.height(16.dp))
                        LabeledTextField(label = "Destination", value = destination, onValueChange = viewModel::updateDestination, placeholder = "Where to?")
                        Spacer(modifier = Modifier.height(16.dp))
                        LabeledTextField(label = "Description", value = description, onValueChange = viewModel::updateDescription, placeholder = "What's the plan?", singleLine = false, modifier = Modifier.height(100.dp))
                    }
                } else {
                    Column {
                        ReadOnlyField("Chat Name", chatName)
                        Spacer(modifier = Modifier.height(16.dp))
                        ReadOnlyField("Destination", destination.ifBlank { "Not set" })
                        Spacer(modifier = Modifier.height(16.dp))
                        ReadOnlyField("Description", description.ifBlank { "No description" })
                    }
                }
            }

            // Linked Itinerary
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("Linked Itinerary", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isOwner) {
                        ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                            TextField(
                                value = selectedTrip?.name ?: "Select a trip...",
                                onValueChange = {}, readOnly = true,
                                modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                colors = TextFieldDefaults.colors(focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3, focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                            )
                            ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }, modifier = Modifier.background(DeepSea2)) {
                                availableTrips.forEach { trip ->
                                    DropdownMenuItem(text = { Text(trip.name, color = DeepSea5) }, onClick = { viewModel.selectTrip(trip); dropdownExpanded = false })
                                }
                            }
                        }
                    } else {
                        ReadOnlyField(value = selectedTrip?.name ?: "None linked")
                    }
                }
            }

            // Member Search & Staged Chips
            if (isOwner) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text("Add New Members", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = friendSearch,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
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

                // Chips for Staged Friends
                if (stagedFriends.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            lazyRowItems(stagedFriends) { friend ->
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(48.dp)).background(DeepSea3).padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatar(
                                        photoUrl = friend.profileImageUrl,
                                        contentDescription = friend.displayName,
                                        modifier = Modifier.size(22.dp),
                                        borderColor = DeepSea3,
                                        backgroundColor = DeepSea2,
                                        placeholderTint = DeepSea5,
                                        borderWidth = 0.dp,
                                        iconSize = 12.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(friend.displayName.split(" ").first(), color = DeepSea5, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Close, null, tint = DeepSea5.copy(alpha = 0.6f), modifier = Modifier.size(16.dp).clickable { viewModel.removeStagedFriend(friend) })
                                }
                            }
                        }
                    }
                }

                // Search Results
                if (filteredFriends.isNotEmpty()) {
                    items(filteredFriends.size, span = { GridItemSpan(maxLineSpan) }) { index ->
                        val friend = filteredFriends[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DeepSea2)
                                .clickable { viewModel.selectFriend(friend) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(
                                photoUrl = friend.profileImageUrl,
                                contentDescription = friend.displayName,
                                modifier = Modifier.size(36.dp),
                                borderColor = DeepSea2,
                                backgroundColor = DeepSea3,
                                placeholderTint = DeepSea5,
                                borderWidth = 0.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(friend.displayName, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Official Members List
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(text = "Current Members ( ${members.size} )", color = DeepSea5, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DeepSea3)
                            .padding(vertical = 8.dp)
                    ) {
                        sortedMembers.forEach { memberId ->
                            val isMe = memberId == currentUid
                            val profile = memberProfiles[memberId]
                            val displayName = if (isMe) "You" else (profile?.first ?: "Loading...")
                            val photoUrl = profile?.second ?: ""
                            val isGroupOwner = memberId == group.ownerId

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileAvatar(
                                    photoUrl = photoUrl,
                                    contentDescription = displayName,
                                    modifier = Modifier.size(40.dp),
                                    borderColor = DeepSea3,
                                    backgroundColor = DeepSea4,
                                    placeholderTint = DeepSea1,
                                    borderWidth = 0.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = displayName, color = DeepSea5, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    if (isGroupOwner) {
                                        Text("Owner", color = DeepSea5.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                }
                                if (isOwner && !isMe) {
                                    IconButton(onClick = { viewModel.removeMember(memberId) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = DeepSea5.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    if (isOwner) {
                        Button(
                            onClick = { viewModel.saveChanges(onComplete = onBackClick) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSea3, disabledContainerColor = DeepSea3.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp), enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(color = DeepSea5, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("Save Changes", color = DeepSea5, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedButton(
                        onClick = { viewModel.leaveChat(onComplete = onNavigateToChats) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp), enabled = !isLoading
                    ) {
                        Text(if (isOwner) "Delete & Leave Chat" else "Leave Chat", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Helpers
@Composable
fun LabeledTextField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", singleLine: Boolean = true) {
    Column(modifier = modifier) {
        Text(label, color = DeepSea5, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, color = DeepSea5.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)), singleLine = singleLine,
            colors = TextFieldDefaults.colors(focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = DeepSea5, focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5)
        )
    }
}

@Composable
fun ReadOnlyField(label: String? = null, value: String) {
    Column {
        if (label != null) {
            Text(text = label, color = DeepSea5, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DeepSea2).padding(16.dp)) {
            Text(text = value, color = DeepSea5, fontSize = 16.sp)
        }
    }
}
