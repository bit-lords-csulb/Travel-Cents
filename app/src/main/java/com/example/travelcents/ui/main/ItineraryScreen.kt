package com.example.travelcents.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.travelcents.data.TripEvent

@Composable
fun ItineraryScreen(
    events: List<TripEvent> = MockItineraryData.sampleEvents
) {
    val eventsByDay = events.groupBy { it.day }

    val days = eventsByDay.keys.toList().sorted()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Dark background covers the whole screen
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
            days.forEach { day ->

                val dailyEvents = eventsByDay[day] ?: emptyList()

                item {
                    Text(
                        text = "Day $day",
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
            .padding(bottom = 24.dp)
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
                    shape = CircleShape,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("✨", fontSize = 16.sp) }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF3B82F6),
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
fun EventCardDispatcher(event: TripEvent) {
    when (event) {
        is TripEvent.Flight -> FlightCard(flight = event)
        is TripEvent.Hotel -> HotelCard(hotel = event)
        is TripEvent.Restaurant -> RestaurantCard(restaurant = event)
        is TripEvent.Activity -> { /* TODO */ }
        is TripEvent.ConcertEvent -> { /* TODO */ }
        is TripEvent.Transport -> { /* TODO */ }
    }
}

@Composable
fun RestaurantCard(restaurant: TripEvent.Restaurant) {
    Card(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🍽️", fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurant.restaurant_name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = "Reservation: ${restaurant.reservation_time}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = restaurant.cuisine,
                    fontSize = 12.sp,
                    color = Color(0xFFE53935),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FlightCard(flight: TripEvent.Flight) {
    Card(modifier = Modifier.fillMaxWidth().height(68.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Text(text = "✈️ Flight: ${flight.airline}", modifier = Modifier.padding(16.dp), color = Color.Black)
    }
}

@Composable
fun HotelCard(hotel: TripEvent.Hotel) {
    Card(modifier = Modifier.fillMaxWidth().height(68.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Text(text = "🏨 Hotel: ${hotel.hotel_name}", modifier = Modifier.padding(16.dp), color = Color.Black)
    }
}

@Preview(showBackground = true)
@Composable
fun ItineraryScreenPreview() {
    ItineraryScreen()
}
