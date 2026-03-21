package com.example.travelcents.ui.main.chats.voting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelcents.data.model.Group
import com.example.travelcents.ui.theme.*

@Composable
fun CreateEventPage(
    group: Group,
    onBackClick: () -> Unit = {},
    onEventCreated: () -> Unit = {},
    viewModel: CreateEventViewModel = viewModel(
        key = group.id,
        factory = CreateEventViewModel.Factory(group.id)
    )
) {
    val title           by viewModel.title.collectAsState()
    val description     by viewModel.description.collectAsState()
    val location        by viewModel.location.collectAsState()
    val time            by viewModel.time.collectAsState()
    val isCreating      by viewModel.isCreating.collectAsState()
    val placeSuggestions by viewModel.placeSuggestions.collectAsState()
    val isLoadingPlaces  by viewModel.isLoadingPlaces.collectAsState()
    val categories       by viewModel.categories.collectAsState()
    var showCustomForm   by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }

    // This will be where we connect the itinerary to the chat to get location for api data
    //LaunchedEffect(group.name) {
    //    viewModel.loadPlaces(group.name)
    //}

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
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DeepSea5
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("New Event", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                        Text("Share a new idea with the squad", fontSize = 12.sp, color = DeepSea5.copy(alpha = 0.5f))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Temporary City Search Bar until we connect itinerary to this
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EventTextField(
                            value = citySearchQuery,
                            onValueChange = { citySearchQuery = it },
                            label = "Where are we going? (e.g. Paris)",
                            icon = Icons.Default.LocationOn,
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = { viewModel.loadPlaces(citySearchQuery) },
                        enabled = citySearchQuery.isNotBlank() && !isLoadingPlaces,
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepSea3,
                            disabledContainerColor = DeepSea3.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Search", color = DeepSea5, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Custom form: expands when Custom Activity is tapped
            if (showCustomForm) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DeepSea2)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EventTextField(
                            value = title,
                            onValueChange = { viewModel.onTitleChange(it) },
                            label = "Event Title *",
                            icon = Icons.Default.Title,
                            singleLine = true
                        )
                        EventTextField(
                            value = description,
                            onValueChange = { viewModel.onDescriptionChange(it) },
                            label = "Description",
                            icon = Icons.Default.Title,
                            singleLine = false,
                            minLines = 3
                        )
                        EventTextField(
                            value = location,
                            onValueChange = { viewModel.onLocationChange(it) },
                            label = "Location",
                            icon = Icons.Default.LocationOn,
                            singleLine = true
                        )
                        EventTextField(
                            value = time,
                            onValueChange = { viewModel.onTimeChange(it) },
                            label = "Time (e.g. 7:00 PM)",
                            icon = Icons.Default.Schedule,
                            singleLine = true
                        )
                        Button(
                            onClick = { viewModel.createEvent(onSuccess = { onEventCreated() }) },
                            enabled = title.isNotBlank() && !isCreating,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepSea3,
                                disabledContainerColor = DeepSea3.copy(alpha = 0.4f)
                            )
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(color = DeepSea5, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Propose Event", color = DeepSea5, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Quick Suggestion: Custom Activity card
            item {
                Text(
                    text = "⚡ QUICK SUGGESTION",
                    color = DeepSea5.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepSea2)
                        .clickable { showCustomForm = !showCustomForm }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepSea3),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DeepSea5)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Custom Activity", color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Type your own idea", color = DeepSea5.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }

            // Place suggestion categories from Foursquare
            if (isLoadingPlaces) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DeepSea5)
                    }
                }
            } else {
                categories.forEach { category ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${category.emoji} ${category.name.uppercase()}",
                                color = DeepSea5.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(placeSuggestions.filter { it.category == category.name }) { place ->
                                PlaceSuggestionCard(
                                    place = place,
                                    onClick = {
                                        viewModel.selectPlace(place)
                                        showCustomForm = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceSuggestionCard(place: PlaceSuggestion, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DeepSea2)
            .clickable { onClick() }
            .padding(bottom = 12.dp)
    ) {
        // Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (place.photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = place.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(DeepSea3))
            }
        }

        // Description
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = place.name,
                color = DeepSea5,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val displayAddress = place.address.split(",").firstOrNull() ?: ""
            Text(
                text = displayAddress.ifEmpty { "Tap to see details" },
                color = DeepSea5.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EventTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = DeepSea5.copy(alpha = 0.7f)) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeepSea5
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepSea5,
            unfocusedBorderColor = DeepSea3,
            focusedTextColor = DeepSea5,
            unfocusedTextColor = DeepSea5,
            cursorColor = DeepSea5
        )
    )
}