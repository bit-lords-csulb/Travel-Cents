package com.example.travelcents.data.media

import com.example.travelcents.BuildConfig
import java.util.Locale

object StaticMapUrlFactory {
    const val PROVIDER_MAPBOX = "mapbox_staticmap"
    const val PROVIDER_OSM = "osm_staticmap"

    val PROVIDER: String
        get() = if (BuildConfig.MAPBOX_TOKEN.isNotBlank()) PROVIDER_MAPBOX else PROVIDER_OSM

    fun buildUrl(
        latitude: Double,
        longitude: Double,
        zoom: Int = 14,
        width: Int = 640,
        height: Int = 360
    ): String {
        val token = BuildConfig.MAPBOX_TOKEN
        return if (token.isNotBlank()) {
            mapboxUrl(latitude, longitude, zoom, width, height, token)
        } else {
            osmUrl(latitude, longitude, zoom, width, height)
        }
    }

    private fun mapboxUrl(
        latitude: Double,
        longitude: Double,
        zoom: Int,
        width: Int,
        height: Int,
        token: String
    ): String {
        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        val pin = "pin-l+e55e5e($lon,$lat)"
        val center = "$lon,$lat,$zoom"
        val size = "${width}x$height@2x"
        return "https://api.mapbox.com/styles/v1/mapbox/streets-v12/static/$pin/$center/$size?access_token=$token"
    }

    private fun osmUrl(
        latitude: Double,
        longitude: Double,
        zoom: Int,
        width: Int,
        height: Int
    ): String {
        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        return "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lon&zoom=$zoom&size=${width}x$height&markers=$lat,$lon,red-pushpin"
    }
}
