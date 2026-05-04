package com.example.travelcents.data.social.repository

import com.example.travelcents.data.social.model.Friend
import com.google.firebase.firestore.DocumentSnapshot

internal fun DocumentSnapshot.displayName(): String {
    val first = getString("firstName") ?: ""
    val last = getString("lastName") ?: ""
    return "$first $last".trim().ifBlank { "Unknown" }
}

private const val ONLINE_STALE_TIMEOUT_MS = 10 * 60 * 1000L

internal fun DocumentSnapshot.isOnlineNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (getBoolean("isOnline") != true) return false
    val lastSeen = getTimestamp("lastSeen")?.toDate()?.time ?: return false
    return nowMillis - lastSeen <= ONLINE_STALE_TIMEOUT_MS
}

internal fun DocumentSnapshot.presenceLabel(
    isOnline: Boolean = isOnlineNow(),
    nowMillis: Long = System.currentTimeMillis()
): String {
    if (isOnline) return "Online"

    val lastSeen = getTimestamp("lastSeen")?.toDate()?.time ?: return "Offline"
    val diffMin = ((nowMillis - lastSeen).coerceAtLeast(0L)) / 60_000
    return when {
        diffMin < 1 -> "Last seen just now"
        diffMin < 60 -> "Last seen ${diffMin}m ago"
        diffMin < 1440 -> "Last seen ${diffMin / 60}h ago"
        else -> "Last seen ${diffMin / 1440}d ago"
    }
}

internal fun DocumentSnapshot.toFriend(): Friend {
    val actuallyOnline = isOnlineNow()

    return Friend(
        uid = id,
        displayName = displayName(),
        email = getString("email") ?: "",
        profileImageUrl = getString("profileImageUrl") ?: "",
        isOnline = actuallyOnline,
        lastSeenLabel = if (actuallyOnline) "online" else "offline"
    )
}
