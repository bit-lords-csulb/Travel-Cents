package com.example.travelcents.notification

data class ChatNotificationTarget(
    val chatType: String,
    val chatId: String
) {
    val notificationKey: String
        get() = "$chatType:$chatId"

    companion object {
        const val TYPE_GROUP = "group"
        const val TYPE_DIRECT = "direct"
        const val EXTRA_CHAT_TYPE = "CHAT_TYPE"
        const val EXTRA_CHAT_ID = "CHAT_ID"

        fun fromExtras(chatType: String?, chatId: String?): ChatNotificationTarget? {
            val normalizedType = when (chatType) {
                TYPE_GROUP, "groups" -> TYPE_GROUP
                TYPE_DIRECT, "directChat", "directChats" -> TYPE_DIRECT
                else -> null
            }
            val normalizedId = chatId?.takeIf { it.isNotBlank() }
            return if (normalizedType != null && normalizedId != null) {
                ChatNotificationTarget(normalizedType, normalizedId)
            } else {
                null
            }
        }
    }
}
