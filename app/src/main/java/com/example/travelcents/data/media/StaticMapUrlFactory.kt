package com.example.travelcents.data.media

import java.util.Locale

object StaticMapUrlFactory {
    const val PROVIDER = "osm_staticmap"

    fun buildUrl(
        latitude: Double,
        longitude: Double,
        zoom: Int = 14,
        width: Int = 640,
        height: Int = 360
    ): String {
        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        return "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lon&zoom=$zoom&size=${width}x$height&markers=$lat,$lon,red-pushpin"
    }
}
