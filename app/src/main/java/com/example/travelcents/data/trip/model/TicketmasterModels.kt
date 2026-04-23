package com.example.travelcents.data.trip.model

import com.google.gson.annotations.SerializedName

data class TmSearchResponse(
    @SerializedName("_embedded") val embedded: TmSearchEmbedded? = null,
    val page: TmPage? = null
)

data class TmSearchEmbedded(
    val events: List<TmEvent> = emptyList()
)

data class TmEvent(
    val id: String = "",
    val name: String = "",
    val url: String? = null,
    val info: String? = null,
    val pleaseNote: String? = null,
    val dates: TmDates? = null,
    val images: List<TmImage> = emptyList(),
    val classifications: List<TmClassification> = emptyList(),
    val priceRanges: List<TmPriceRange>? = null,
    @SerializedName("_embedded") val embedded: TmEventEmbedded? = null
)

data class TmDates(
    val start: TmDatePoint? = null,
    val end: TmDatePoint? = null,
    val timezone: String? = null,
    val status: TmStatus? = null
)

data class TmDatePoint(
    val localDate: String? = null,
    val localTime: String? = null,
    val dateTime: String? = null
)

data class TmStatus(
    val code: String? = null
)

data class TmImage(
    val ratio: String? = null,
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val fallback: Boolean = false
)

data class TmClassification(
    val primary: Boolean = false,
    val segment: TmNamedNode? = null,
    val genre: TmNamedNode? = null,
    val subGenre: TmNamedNode? = null
)

data class TmNamedNode(
    val id: String? = null,
    val name: String? = null
)

data class TmPriceRange(
    val type: String? = null,
    val currency: String? = null,
    val min: Double? = null,
    val max: Double? = null
)

data class TmEventEmbedded(
    val venues: List<TmVenue> = emptyList()
)

data class TmVenue(
    val id: String = "",
    val name: String = "",
    val url: String? = null,
    val timezone: String? = null,
    val city: TmCity? = null,
    val country: TmCountry? = null,
    val address: TmAddress? = null,
    val location: TmLocation? = null
)

data class TmCity(
    val name: String? = null
)

data class TmCountry(
    val name: String? = null,
    val countryCode: String? = null
)

data class TmAddress(
    val line1: String? = null,
    val line2: String? = null
)

data class TmLocation(
    val longitude: String? = null,
    val latitude: String? = null
)

data class TmPage(
    val size: Int = 0,
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val number: Int = 0
)
