package com.example.travelcents.data.trip.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationTimeZonesTest {

    @Test
    fun resolveTimeZoneId_usesAirportWhenAvailable() {
        val resolved = DestinationTimeZones.resolveTimeZoneId(
            destination = "Paris, France",
            destinationIata = "LAX"
        )

        assertEquals("America/Los_Angeles", resolved)
    }

    @Test
    fun resolveTimeZoneId_usesCityAndCountryWithoutCrashing() {
        val resolved = DestinationTimeZones.resolveTimeZoneId(
            destination = "Paris, France"
        )

        assertEquals("Europe/Paris", resolved)
    }

    @Test
    fun resolveTimeZoneId_returnsNullForBlankDestination() {
        val resolved = DestinationTimeZones.resolveTimeZoneId(
            destination = "   "
        )

        assertNull(resolved)
    }
}
