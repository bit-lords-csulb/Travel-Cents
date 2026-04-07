package com.example.travelcents.data.model

import com.google.firebase.Timestamp

data class DirectChatPreview(
    val id: String = "",
    val otherUid: String = "",
    val otherUserName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null
)