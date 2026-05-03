package com.example.travelcents.data.social.model

import com.google.firebase.Timestamp

data class Group(
    val id: String = "",
    val name: String = "",
    val destination: String = "",
    val description: String = "",
    val members: List<String> = emptyList(),
    val ownerId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val groupImageUrl: String = "",
    val linkedTripId: String = "",
    val linkedTripOwnerId: String = ""
)

