package com.example.travelcents.data.user.model

data class CurrentUserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val profileImageSource: String = "",
    val doNotShareData: Boolean = false,
    val showWeeklySummary: Boolean = true,
    // Regional Settings
    val country: String = "United States",
    val region: String = "California",
    val city: String = "Long Beach",
    val isLoading: Boolean = true
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifEmpty { "User" }
}
