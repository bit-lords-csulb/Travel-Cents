package com.example.travelcents.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.MockItineraryData
import com.example.travelcents.data.model.TravelEvent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ItineraryScreen(
    tripId: String = "3fa85f64-5717-4562-b3fc-2c963f66afa6", // We will pass a real ID here later
    viewModel: ItineraryViewModel = viewModel()
) {
    // 1. Observe the "bucket" from the ViewModel
    val events by viewModel.events.collectAsState()

    // 2. Turn on the faucet the moment the screen loads
    LaunchedEffect(tripId) {
        viewModel.fetchItinerary(tripId)
    }

    // 3. Keep the grouping logic exactly the same!
    val eventsByDay = events.groupBy { it.date }
    val sortedDates = eventsByDay.keys.toList().sorted()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(top = 24.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            TripHeader(tripName = MockItineraryData.sampleTrip.trip_name)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color(0xFF0D1B2A)),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 0.dp,
                start = 24.dp,
                end = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sortedDates.forEach { date ->

                // Grab the events for this specific date
                val dailyEvents = eventsByDay[date] ?: emptyList()

                item {
                    Text(
                        text = date, // This will now show "2026-03-20" or whatever the database holds
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(dailyEvents) { event ->
                    EventCardDispatcher(event = event)
                }
            }
        }
    }
}

@Composable
fun TripHeader(tripName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 1. ROW WITH TRIP NAME AND DATE
            Column {
                Text(
                    text = tripName.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "FEB 10 - FEB 13",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFF1E293B), // That deep navy background
                    border = BorderStroke(1.dp, Color(0xFF334155)) // Subtle border from your image
                ) {
                    IconButton(onClick = { /* Handle AI/Magic action */ }) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = Color(0xFF94A3B8), // Muted blue icon color
                            modifier = Modifier.size(20.dp) // Making the icon slightly smaller inside
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF415A77),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 24.sp) }
                }
            }
        }

        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "SWITCH TO CALENDAR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp), // Space between the buttons and the line
            thickness = 1.dp,
            color = Color(0xFF1E293B) // A muted slate gray to match the dark theme
        )
    }
}

@Composable
fun EventCardDispatcher(event: TravelEvent) {
    when (event.type.lowercase()) {
        "flight" -> FlightCard(event = event)
        "hotel" -> HotelCard(event = event)
        "restaurant" -> RestaurantCard(event = event)
        else -> { /* Handle others */ }
    }
}

@Composable
fun FlightCard(event: TravelEvent) {
    Card(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2438))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(Color(0xFFEC4899)))

            Column(modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
                Text(text = event.startTime, fontSize = 10.sp, color = Color(0xFF94A3B8))

                // 3. Fix: Pull from the details map
                val destination = event.details["destination_airport"] ?: "Destination"
                val airline = event.details["airline"] ?: ""
                val flightNo = event.details["flight_number"] ?: ""

                Text(
                    text = "Flight to $destination",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(text = "$airline $flightNo", fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
    }
}


@Composable
fun HotelCard(event: TravelEvent) {
    Card(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3535)) // Dark teal background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 1. The Accent Strip (Bright Cyan for Hotels)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color(0xFF06B6D4))
            )

            Column(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Using the main TravelEvent startTime instead of a hardcoded string
                Text(text = "Check-in: ${event.startTime}", fontSize = 10.sp, color = Color(0xFF94A3B8))

                // Pulling the specific hotel name from the details map
                val hotelName = event.details["hotel_name"] ?: "Unknown Hotel"

                Text(
                    text = "Hotel Check-in",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(text = hotelName, fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun RestaurantCard(event: TravelEvent) {
    Card(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3324)) // Dark olive background
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 1. The Accent Strip (Yellow/Gold for Food)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color(0xFFEAB308))
            )

            Column(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Using the main TravelEvent startTime
                Text(text = event.startTime, fontSize = 10.sp, color = Color(0xFF94A3B8))

                // Pulling specific restaurant info from the details map
                val restaurantName = event.details["restaurant_name"] ?: "Unknown Restaurant"
                val cuisine = event.details["cuisine"] ?: ""

                Text(
                    text = restaurantName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(text = cuisine, fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun ItineraryScreenPreview() {
//    ItineraryScreen()
//}
