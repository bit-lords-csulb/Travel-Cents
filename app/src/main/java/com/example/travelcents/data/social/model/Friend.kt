package com.example.travelcents.data.social.model

data class Friend(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val isOnline: Boolean = false,
    val lastSeenLabel: String = ""
)
