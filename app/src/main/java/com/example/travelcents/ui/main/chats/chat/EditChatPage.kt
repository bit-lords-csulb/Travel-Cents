package com.example.travelcents.ui.main.chats.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
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
        factory = EditChatViewModel.Factory(group)
    )
) {
    val isOwner = viewModel.isOwner

    val chatName by viewModel.chatName.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val members by viewModel.members.collectAsState()
    val memberNames by viewModel.memberNames.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val availableTrips by viewModel.availableTrips.collectAsState()
    val selectedTrip by viewModel.selectedTrip.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    val currentUid = Firebase.auth.currentUser?.uid ?: ""

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.updateImageUri(uri)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DeepSea1)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepSea5)
                }
                Text(text = "Edit Chat", color = DeepSea5, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
        ) {
            // Profile Picture
            item {
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
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Input Fields
            item {
                if (isOwner) {
                    EditTextField(label = "Chat Name", value = chatName, onValueChange = viewModel::updateName)
                    Spacer(modifier = Modifier.height(16.dp))
                    EditTextField(label = "Destination", value = destination, onValueChange = viewModel::updateDestination)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Description", color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = description,
                        onValueChange = viewModel::updateDescription,
                        modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5)
                    )
                } else {
                    ReadOnlyField("Chat Name", chatName)
                    Spacer(modifier = Modifier.height(16.dp))
                    ReadOnlyField("Destination", destination.ifBlank { "Not set" })
                    Spacer(modifier = Modifier.height(16.dp))
                    ReadOnlyField("Description", description.ifBlank { "No description" })
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Linked Itinerary
                Text("Linked Itinerary", color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Members section
            item {
                Text(text = "Members ( ${members.size} )", color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DeepSea3).padding(vertical = 8.dp)) {
                    members.forEach { memberId ->
                        val isMe = memberId == currentUid
                        val displayName = if (isMe) "You" else (memberNames[memberId] ?: "Loading...")
                        val isGroupOwner = memberId == group.ownerId

                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(DeepSea4), contentAlignment = Alignment.Center) {
                                Text(text = if (isMe) "Y" else displayName.take(1).uppercase(), color = DeepSea1, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = displayName, color = DeepSea5, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                if (isGroupOwner) {
                                    Text("Owner", color = DeepSea5.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                            }
                            if (isOwner && !isMe) {
                                IconButton(onClick = { viewModel.removeMember(memberId) }) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = DeepSea5.copy(alpha = 0.5f)) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Buttons
            item {
                if (isOwner) {
                    Button(
                        onClick = { viewModel.saveChanges(onComplete = onBackClick) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepSea4, disabledContainerColor = DeepSea4.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(25.dp), enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = DeepSea1, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Text("Save Changes", color = DeepSea1, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedButton(
                    onClick = {
                        viewModel.leaveChat(onComplete = onNavigateToChats)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(25.dp),
                    enabled = !isLoading
                ) {
                    Text(if (isOwner) "Delete & Leave Chat" else "Leave Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(text = label, color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5
            )
        )
    }
}

@Composable
fun ReadOnlyField(label: String? = null, value: String) {
    Column {
        if (label != null) {
            Text(text = label, color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DeepSea2).padding(16.dp)
        ) {
            Text(text = value, color = DeepSea5, fontSize = 16.sp)
        }
    }
}