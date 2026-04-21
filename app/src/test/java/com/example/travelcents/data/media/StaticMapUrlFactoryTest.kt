package com.example.travelcents.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticMapUrlFactoryTest {

    @Test
    fun buildUrl_formatsExpectedProviderAndCoordinates() {
        val url = StaticMapUrlFactory.buildUrl(
            latitude = 33.7701,
            longitude = -118.1937,
            zoom = 15,
            width = 800,
            height = 450
        )

        assertEquals("osm_staticmap", StaticMapUrlFactory.PROVIDER)
        assertTrue(url.contains("center=33.770100,-118.193700"))
        assertTrue(url.contains("zoom=15"))
        assertTrue(url.contains("size=800x450"))
        assertTrue(url.contains("markers=33.770100,-118.193700"))
    }
}
