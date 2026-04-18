package com.example.travelcents.ui.main.chats.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.travelcents.data.model.Group
import com.example.travelcents.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChatPage(
    group: Group,
    onBackClick: () -> Unit,
    viewModel: EditChatViewModel = viewModel(
        factory = EditChatViewModel.Factory(group)
    )
) {
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

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.updateImageUri(uri)
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
                .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepSea5)
                }
                Text(
                    text = "Edit Chat",
                    color = DeepSea5,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
        ) {
            // Group Chat Picture
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(100.dp)) {
                        // Image Display
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(DeepSea3)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Group Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (group.groupImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = group.groupImageUrl,
                                    contentDescription = "Current Group Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = chatName.take(2).uppercase(),
                                    color = DeepSea5,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Camera Icon Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DeepSea4)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Edit Image", tint = DeepSea1, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Input Fields
            item {
                EditTextField(label = "Chat Name", value = chatName, onValueChange = viewModel::updateName)
                Spacer(modifier = Modifier.height(16.dp))
                EditTextField(label = "Destination", value = destination, onValueChange = viewModel::updateDestination)
                Spacer(modifier = Modifier.height(16.dp))

                // Description (Multi-line)
                Text("Description", color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                TextField(
                    value = description,
                    onValueChange = viewModel::updateDescription,
                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Linked Itinerary Dropdown
                Text("Linked Itinerary", color = DeepSea5, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    TextField(
                        value = selectedTrip?.name ?: "Select a trip...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(DeepSea2)
                    ) {
                        availableTrips.forEach { trip ->
                            DropdownMenuItem(
                                text = { Text(trip.name, color = DeepSea5) },
                                onClick = {
                                    viewModel.selectTrip(trip)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Members section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DeepSea3)
                        .padding(vertical = 8.dp)
                ) {
                    members.forEach { memberId ->
                        val isMe = memberId == currentUid
                        // Look up the name from our map, fallback to "Loading..." while fetching
                        val displayName = if (isMe) "You" else (memberNames[memberId] ?: "Loading...")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(DeepSea4),
                                contentAlignment = Alignment.Center
                            ) {
                                // Update the initial to use the fetched display name
                                Text(
                                    text = if (isMe) "Y" else displayName.take(1).uppercase(),
                                    color = DeepSea1,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = displayName, // USE THE DISPLAY NAME HERE
                                color = DeepSea5,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isMe) {
                                IconButton(onClick = { viewModel.removeMember(memberId) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = DeepSea5.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Save Button
            item {
                Button(
                    onClick = { viewModel.saveChanges(onComplete = onBackClick) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepSea4,
                        disabledContainerColor = DeepSea4.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(25.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = DeepSea1,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes", color = DeepSea1, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
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