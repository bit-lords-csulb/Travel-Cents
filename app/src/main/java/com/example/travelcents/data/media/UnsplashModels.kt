package com.example.travelcents.data.media

import com.google.gson.annotations.SerializedName

data class UnsplashSearchResponse(
    val results: List<UnsplashPhoto> = emptyList()
)

data class UnsplashPhoto(
    val id: String? = null,
    val urls: UnsplashPhotoUrls? = null,
    @SerializedName("alt_description") val altDescription: String? = null,
    val user: UnsplashUser? = null,
    val links: UnsplashLinks? = null
)

data class UnsplashPhotoUrls(
    val raw: String? = null,
    val full: String? = null,
    val regular: String? = null,
    val small: String? = null,
    val thumb: String? = null
)

data class UnsplashUser(
    val name: String? = null
)

data class UnsplashLinks(
    val html: String? = null
)
