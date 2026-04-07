package com.example.travelcents.ui.main.newtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.R
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5

private data class InterestItem(val key: String, val label: String, val imageRes: Int)

private val interestItems = listOf(
    InterestItem("culture",      "Culture & Museums",   R.drawable.interest_culture),
    InterestItem("food",         "Food & Dining",        R.drawable.interest_food),
    InterestItem("nature",       "Beach & Nature",       R.drawable.interest_nature),
    InterestItem("adventure",    "Adventure",            R.drawable.interest_adventure),
    InterestItem("nightlife",    "Nightlife",            R.drawable.interest_nightlife),
    InterestItem("shopping",     "Shopping",             R.drawable.interest_shopping),
    InterestItem("history",      "History",              R.drawable.interest_history),
    InterestItem("architecture", "Architecture",         R.drawable.interest_architecture),
    InterestItem("photography",  "Photography",          R.drawable.interest_photography),
    InterestItem("wellness",     "Wellness & Spa",       R.drawable.interest_wellness),
    InterestItem("hiking",       "Hiking",               R.drawable.interest_hiking),
    InterestItem("watersports",  "Water Sports",         R.drawable.interest_watersports),
    InterestItem("wildlife",     "Wildlife & Safaris",   R.drawable.interest_wildlife),
    InterestItem("music",        "Music & Festivals",    R.drawable.interest_music),
    InterestItem("art",          "Art Galleries",        R.drawable.interest_art),
    InterestItem("markets",      "Local Markets",        R.drawable.interest_markets),
    InterestItem("themeparks",   "Theme Parks",          R.drawable.interest_themeparks),
    InterestItem("roadtrips",    "Road Trips",           R.drawable.interest_roadtrips),
    InterestItem("skiing",       "Skiing & Snow",        R.drawable.interest_skiing),
    InterestItem("volunteering", "Volunteering",         R.drawable.interest_volunteering)
)

private val interestVocabulary = listOf(
    "Surfing", "Diving", "Snorkeling", "Cycling", "Yoga", "Meditation", "Cooking Classes",
    "Wine Tasting", "Beer Tours", "Street Food", "Karaoke", "Camping", "Rock Climbing",
    "Paragliding", "Bungee Jumping", "Hot Air Balloon", "Cruises", "River Rafting",
    "Horse Riding", "Ziplining", "Kayaking", "Paddleboarding", "Fishing", "Hunting",
    "Birdwatching", "Stargazing", "Casino", "Golf", "Tennis", "Bowling",
    "Escape Rooms", "Comedy Shows", "Theater", "Opera", "Ballet", "Jazz Clubs",
    "Antique Markets", "Flea Markets", "Outlet Shopping", "Luxury Shopping",
    "Spa Days", "Massage", "Hot Springs", "Sauna", "CrossFit", "Marathon",
    "Triathlon", "Skateboarding", "BMX", "Motocross", "Go-Karting", "F1 Racing",
    "Safari Drives", "Whale Watching", "Dolphin Watching", "Turtle Watching",
    "Volcano Hiking", "Glacier Hiking", "Desert Safari", "Jungle Trek",
    "City Walking Tours", "Ghost Tours", "Food Tours", "Pub Crawls",
    "Language Classes", "Dance Classes", "Pottery", "Painting", "Photography Tours",
    "Film Locations", "Sports Events", "Concerts", "Street Art Tours",
    "Architecture Tours", "Religious Sites", "Temples", "Monasteries",
    "Battlefield Tours", "Cold War Sites", "Catacombs", "Underground Cities"
)

@Composable
fun TripStep5InterestsPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onTripGenerated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage = (uiState as? TripUiState.Error)?.message
    var searchQuery by remember { mutableStateOf("") }
    var customInterests by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Top bar
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF010E24))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TripWizardColors.Blue)
                    }
                    Text("Step 5 of 5", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                }
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, "Close", tint = TripWizardColors.Blue)
                }
            }
            // Full progress bar (100%)
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(listOf(TripWizardColors.Blue, TripWizardColors.PrimaryContainer)))
            )
        }

        // Grid content
        val filteredItems = if (searchQuery.length >= 2) {
            interestItems.filter {
                it.label.contains(searchQuery, ignoreCase = true) || it.key.contains(searchQuery, ignoreCase = true)
            }
        } else interestItems

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress widget
            item(span = { GridItemSpan(2) }) {
                S5ProgressWidget()
            }

            // Hero
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        "PERSONALIZATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TripWizardColors.OnSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "What interests you?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepSea5,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${viewModel.interests.size} selected",
                            fontSize = 12.sp,
                            color = if (viewModel.interests.size >= 1) TripWizardColors.Blue else TripWizardColors.OnSurfaceVariant
                        )
                        Text("Select at least 1", fontSize = 12.sp, color = TripWizardColors.OnSurfaceVariant)
                    }
                }
            }

            // Search Bar
            item(span = { GridItemSpan(2) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TripWizardColors.ContainerHigh),
                        placeholder = { Text("Search interests...", color = TripWizardColors.OnSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TripWizardColors.OnSurfaceVariant) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = TripWizardColors.Blue
                        ),
                        singleLine = true
                    )

                    val showAddChip = searchQuery.length >= 2 &&
                            filteredItems.isEmpty() &&
                            customInterests.none { it.equals(searchQuery, ignoreCase = true) }

                    if (showAddChip) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, TripWizardColors.Blue.copy(0.5f), RoundedCornerShape(999.dp))
                                .clickable {
                                    val key = searchQuery.lowercase().replace(" ", "_")
                                    if (key !in customInterests) {
                                        customInterests = customInterests + searchQuery
                                        viewModel.toggleInterest(key)
                                    }
                                    searchQuery = ""
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+ Add '$searchQuery'", color = TripWizardColors.Blue, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Custom interests
            if (customInterests.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column {
                        Text(
                            "YOUR INTERESTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripWizardColors.OnSurfaceVariant,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customInterests.forEach { interest ->
                                Row(
                                    modifier = Modifier
                                        .background(TripWizardColors.Blue.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                                        .border(1.dp, TripWizardColors.Blue.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(interest, color = TripWizardColors.Blue, fontSize = 12.sp)
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        modifier = Modifier.size(14.dp).clickable {
                                            val key = interest.lowercase().replace(" ", "_")
                                            customInterests = customInterests.filter { it != interest }
                                            viewModel.toggleInterest(key)
                                        },
                                        tint = TripWizardColors.Blue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF9F0519).copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Text(errorMessage, color = Color(0xFFFF716C), fontSize = 13.sp)
                    }
                }
            }

            // Interest cards
            items(filteredItems) { item ->
                val selected = item.key in viewModel.interests
                S5InterestCard(
                    item = item,
                    selected = selected,
                    onClick = { viewModel.toggleInterest(item.key) }
                )
            }

s            item(span = { GridItemSpan(2) }) {
                Spacer(Modifier.height(4.dp))
            }
        }

        // Plan My Trip button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = {
                    if (viewModel.origin.isBlank()) viewModel.origin = "Not specified"
                    viewModel.generateTrip()
                    onTripGenerated()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = viewModel.interests.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripWizardColors.Blue,
                    contentColor = Color(0xFF001627),
                    disabledContainerColor = TripWizardColors.Blue.copy(alpha = 0.25f),
                    disabledContentColor = DeepSea5.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Continue to Generate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun S5ProgressWidget() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TripWizardColors.ContainerHigh.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YOUR PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.OnSurfaceVariant, letterSpacing = 1.5.sp)
                Box(
                    modifier = Modifier
                        .background(TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("100%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.Blue)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TripWizardColors.Blue)
                    )
                }
            }
        }
    }
}

@Composable
private fun S5InterestCard(
    item: InterestItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(TripWizardColors.ContainerHigh)
            .then(
                if (selected)
                    Modifier.border(2.dp, TripWizardColors.Blue.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(item.imageRes),
            contentDescription = item.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (selected) 0.8f else 0.6f
        )
        // Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                        startY = 80f,
                        endY = Float.MAX_VALUE
                    )
                )
        )
        // Selected tint
        if (selected) {
            Box(modifier = Modifier.fillMaxSize().background(TripWizardColors.Blue.copy(alpha = 0.1f)))
        }
        // Label
        Text(
            text = item.label,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DeepSea5
        )
        // Check indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) TripWizardColors.Blue else Color.White.copy(alpha = 0.1f))
                .border(1.5.dp, if (selected) TripWizardColors.Blue else Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, null, tint = Color(0xFF001627), modifier = Modifier.size(12.dp))
            }
        }
    }
}
