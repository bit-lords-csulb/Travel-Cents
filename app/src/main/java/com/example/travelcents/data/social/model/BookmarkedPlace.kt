package com.example.travelcents.data.social.model

data class BookmarkedPlace(
    val id: String,
    val name: String,
    val category: String,
    val area: String,
    val imageUrl: String?,
    val yelpUrl: String?,
    val savedAtEpochMs: Long
)