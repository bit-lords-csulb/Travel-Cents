package com.example.travelcents.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null,
    // "text" (default) or "trip_card"
    val messageType: String = "text",
    val sharedTripId: String? = null,
    val ownerUid: String? = null,
    val tripName: String? = null,
    val tripDestination: String? = null,
    val tripDateFrom: String? = null,
    val tripDateTo: String? = null,
    val coverImageUrl: String? = null
)