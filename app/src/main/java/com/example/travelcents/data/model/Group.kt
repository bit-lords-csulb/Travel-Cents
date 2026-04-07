package com.example.travelcents.data.model

import com.google.firebase.Timestamp

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val groupImageUrl: String = "",
    val linkedTripId: String = "",
    val linkedTripOwnerId: String = ""
)