package com.example.travelcents.ui.main.chats.voting

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelcents.data.model.Group
import com.example.travelcents.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateEventPage(
    group: Group,
    onBackClick: () -> Unit = {},
    onEventCreated: () -> Unit = {},
    viewModel: CreateEventViewModel = viewModel(factory = CreateEventViewModel.Factory(group.id))
) {
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val location by viewModel.location.collectAsState()
    val time by viewModel.time.collectAsState()
    val photoUrl by viewModel.photoUrl.collectAsState()

    val placeSuggestions by viewModel.placeSuggestions.collectAsState()
    val isLoadingPlaces by viewModel.isLoadingPlaces.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()

    var showCustomForm by remember { mutableStateOf(false) }
    var businessQuery by remember { mutableStateOf("") }
    var cityQuery by remember { mutableStateOf("") }

    var isPickingTime by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleLocalImage(it) }
    }

    //
    BackHandler {
        viewModel.clearForm()
        viewModel.clearSuggestions()
        onBackClick()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(DeepSea1)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .padding(top = 48.dp, bottom = 20.dp)
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DeepSea5,
                    modifier = Modifier
                        .size(28.dp)
                )
            }

            // New Event Title
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("New Event", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                Text(
                    "Share a new idea with the squad",
                    fontSize = 10.sp,
                    color = DeepSea5.copy(alpha = 0.5f)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            // Search Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EventTextField(
                        businessQuery,
                        { businessQuery = it },
                        "Search activity (e.g. Sushi)",
                        Icons.Default.Search
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            EventTextField(
                                cityQuery,
                                { cityQuery = it },
                                "Where to?",
                                Icons.Default.LocationOn
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.loadPlaces(businessQuery, cityQuery) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepSea3)
                        ) {
                            if (isLoadingPlaces) CircularProgressIndicator(
                                color = DeepSea5,
                                modifier = Modifier.size(24.dp)
                            )
                            else Icon(Icons.Default.TravelExplore, null, tint = DeepSea5)
                        }
                    }
                }
            }

            // Custom Activity Form
            if (showCustomForm) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DeepSea2)
                            .border(1.dp, DeepSea3.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Draft Details",
                                color = DeepSea5,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            if (title.isNotEmpty() || description.isNotEmpty() || location.isNotEmpty() || time.isNotEmpty() || photoUrl.isNotEmpty()) {
                                Text(
                                    text = "Clear",
                                    color = DeepSea3,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.clearForm() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        EventTextField(
                            title,
                            { viewModel.onTitleChange(it) },
                            "Title",
                            Icons.Default.Title
                        )
                        EventTextField(
                            description,
                            { viewModel.onDescriptionChange(it) },
                            "Event Details",
                            Icons.Default.Description,
                            singleLine = false
                        )
                        EventTextField(
                            location,
                            { viewModel.onLocationChange(it) },
                            "Address",
                            Icons.Default.Map
                        )
                        EventTextField(
                            value = time,
                            onValueChange = {},
                            label = "Proposed Time",
                            icon = Icons.Default.Schedule,
                            modifier = Modifier.clickable { isPickingTime = !isPickingTime },
                            enabled = false
                        )

                        AnimatedVisibility(visible = isPickingTime) {
                            DeepSeaTimePicker(onTimeSelected = { viewModel.onTimeChange(it) })
                        }

                        // Upload Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepSea1.copy(alpha = 0.5f))
                                .border(1.dp, DeepSea3, RoundedCornerShape(12.dp))
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUrl.isEmpty()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        null,
                                        tint = DeepSea5.copy(0.4f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Add Event Photo",
                                        color = DeepSea5.copy(0.4f),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.createEvent { onEventCreated() } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSea3)
                        ) {
                            if (isCreating) CircularProgressIndicator(
                                color = DeepSea5,
                                modifier = Modifier.size(20.dp)
                            )
                            else Text(
                                "Propose Event",
                                color = DeepSea5,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Toggle Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepSea2)
                        .clickable {
                            val isOpening = !showCustomForm
                            showCustomForm = isOpening
                            if (isOpening) {
                                viewModel.clearForm()
                                coroutineScope.launch {
                                    delay(50)
                                    listState.animateScrollToItem(1)
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showCustomForm) Icons.Default.RemoveCircle else Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = DeepSea5,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (showCustomForm) "Hide Details" else "Create Custom Activity",
                        color = DeepSea5,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Get Yelp Results
            val grouped = placeSuggestions.groupBy { it.category }
            grouped.forEach { (cat, places) ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cat.uppercase(),
                            color = DeepSea5.copy(0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )

                        if (places.size > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Swipe",
                                    color = DeepSea5.copy(0.3f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = DeepSea5.copy(0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(places) { place ->
                            PlaceSuggestionCard(place) {
                                viewModel.selectPlace(place)
                                showCustomForm = true
                                coroutineScope.launch {
                                    delay(50)
                                    listState.animateScrollToItem(1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers
@Composable
fun PlaceSuggestionCard(place: PlaceSuggestion, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DeepSea2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            AsyncImage(
                model = place.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    place.name,
                    color = DeepSea5,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    place.interest,
                    color = DeepSea3,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    place.address,
                    color = DeepSea5.copy(0.4f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EventTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = DeepSea5.copy(0.5f), fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = DeepSea5) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepSea3,
            unfocusedBorderColor = DeepSea3,
            focusedTextColor = DeepSea5,
            unfocusedTextColor = DeepSea5,
            focusedContainerColor = DeepSea1.copy(alpha = 0.5f),
            unfocusedContainerColor = DeepSea1.copy(alpha = 0.5f),
            disabledBorderColor = DeepSea3,
            disabledTextColor = DeepSea5,
            disabledLabelColor = DeepSea5.copy(0.5f),
            disabledLeadingIconColor = DeepSea5,
            disabledContainerColor = DeepSea1.copy(alpha = 0.5f)
        )
    )
}


@Composable
fun DeepSeaTimePicker(
    onTimeSelected: (String) -> Unit
) {
    val hours = (1..12).map { it.toString() }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }
    val amPm = listOf("AM", "PM")
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val totalPageCount = 10000

    // LANDING ON 12:00
    // 5003 % 12 = 11 (The index for "12")
    val hourState = rememberPagerState(initialPage = 5003) { totalPageCount }

    // 5040 % 60 = 0 (The index for "00")
    val minuteState = rememberPagerState(initialPage = 5040) { totalPageCount }

    // Start on PM (Index 1) or AM (Index 0)
    val amPmState = rememberPagerState(initialPage = 0) { amPm.size }

    LaunchedEffect(hourState.currentPage, minuteState.currentPage, amPmState.currentPage) {
        val h = hours[hourState.currentPage % hours.size]
        val m = minutes[minuteState.currentPage % minutes.size]
        val ap = amPm[amPmState.currentPage % amPm.size]
        onTimeSelected("$h:$m $ap")
    }
    // Haptic feedback on scroll
    LaunchedEffect(hourState.currentPage) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }

    LaunchedEffect(minuteState.currentPage) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }

    LaunchedEffect(amPmState.currentPage) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(DeepSea1.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(DeepSea3.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            InfiniteWheelPickerColumn(
                items = hours,
                state = hourState,
                modifier = Modifier.weight(1f)
            )
            Text(":", color = DeepSea5, fontWeight = FontWeight.Bold)
            InfiniteWheelPickerColumn(
                items = minutes,
                state = minuteState,
                modifier = Modifier.weight(1f)
            )

            WheelPickerColumn(items = amPm, state = amPmState, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun InfiniteWheelPickerColumn(
    items: List<String>,
    state: PagerState,
    modifier: Modifier = Modifier
) {
    VerticalPager(
        state = state,
        modifier = modifier.height(120.dp),
        contentPadding = PaddingValues(vertical = 40.dp)
    ) { page ->
        // Map the infinite page index
        val index = page % items.size
        val isSelected = (state.currentPage % items.size) == index

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = items[index],
                fontSize = if (isSelected) 20.sp else 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) DeepSea5 else DeepSea5.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun WheelPickerColumn(
    items: List<String>,
    state: PagerState,
    modifier: Modifier = Modifier
) {
    VerticalPager(
        state = state,
        modifier = modifier.height(120.dp),
        contentPadding = PaddingValues(vertical = 40.dp)
    ) { page ->
        // Calculate how far the item is from the center to scale it
        val isSelected = state.currentPage == page

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = items[page],
                fontSize = if (isSelected) 20.sp else 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) DeepSea5 else DeepSea5.copy(alpha = 0.3f)
            )
        }
    }
}