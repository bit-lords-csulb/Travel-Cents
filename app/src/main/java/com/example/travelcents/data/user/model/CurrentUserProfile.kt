package com.example.travelcents.data.user.model

data class CurrentUserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val profileImageSource: String = "",
    val isLoading: Boolean = true
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifEmpty { "User" }
}


