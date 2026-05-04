package com.example.travelcents.data.social.model

import com.google.firebase.Timestamp

data class DirectChatPreview(
    val id: String = "",
    val otherUid: String = "",
    val otherUserName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val lastSenderId: String = "",
    val lastSenderName: String = "",
    val otherPhotoUrl: String = ""
)

