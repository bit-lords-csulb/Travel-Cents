package com.example.travelcents.data.model

import com.google.firebase.Timestamp

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val time: String = "",
    val photoUrl: String = "",
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Timestamp? = null,
    val upvotes: List<String> = emptyList(),
    val downvotes: List<String> = emptyList(),
    val commentCount: Int = 0
)

data class EventComment(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null
)