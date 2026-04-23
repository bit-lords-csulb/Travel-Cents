package com.example.travelcents.data.social.repository

import com.example.travelcents.data.social.model.Friend
import com.google.firebase.firestore.DocumentSnapshot

internal fun DocumentSnapshot.displayName(): String {
    val first = getString("firstName") ?: ""
    val last = getString("lastName") ?: ""
    return "$first $last".trim().ifBlank { "Unknown" }
}

internal fun DocumentSnapshot.toFriend(): Friend = Friend(
    uid = id,
    displayName = displayName(),
    email = getString("email") ?: "",
    profileImageUrl = getString("profileImageUrl") ?: "",
    isOnline = getBoolean("isOnline") ?: false,
    lastSeenLabel = "Offline"
)
