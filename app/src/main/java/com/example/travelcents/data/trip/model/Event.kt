package com.example.travelcents.data.trip.model

import com.google.firebase.Timestamp

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val time: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val photoUrl: String = "",
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Timestamp? = null,
    val upvotes: List<String> = emptyList(),
    val downvotes: List<String> = emptyList(),
    val commentCount: Int = 0,
    val isWon: Boolean = false
)

data class EventComment(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null
)

