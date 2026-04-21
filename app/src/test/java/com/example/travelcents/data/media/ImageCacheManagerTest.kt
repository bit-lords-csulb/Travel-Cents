package com.example.travelcents.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCacheManagerTest {

    @Test
    fun cacheFileNameForUrl_preservesExtensionAndUsesFullUrlHash() {
        val baseUrl = "https://img.example.com/hotel/photo.jpg"
        val resizedUrl = "https://img.example.com/hotel/photo.jpg?width=1200"

        val baseFileName = ImageCacheManager.cacheFileNameForUrl(baseUrl)
        val resizedFileName = ImageCacheManager.cacheFileNameForUrl(resizedUrl)

        assertTrue(baseFileName.endsWith(".jpg"))
        assertTrue(resizedFileName.endsWith(".jpg"))
        assertNotEquals(baseFileName, resizedFileName)
    }

    @Test
    fun cacheFileNameForUrl_fallsBackToImgWhenUrlHasNoUsableExtension() {
        val url = "https://maps.example.com/staticmap?center=1,2&zoom=14"

        val fileName = ImageCacheManager.cacheFileNameForUrl(url)

        assertTrue(fileName.endsWith(".img"))
    }

    @Test
    fun remoteMediaUrls_deduplicatesHeroAndGalleryUrls() {
        val event = com.example.travelcents.data.trip.model.TravelEvent(
            eventId = "event-1",
            type = "hotel",
            itineraryId = "trip-1",
            imageUrl = "https://img.example.com/hero.jpg",
            photoUrls = listOf(
                "https://img.example.com/hero.jpg",
                "https://img.example.com/gallery-1.jpg",
                "https://img.example.com/gallery-1.jpg"
            )
        )

        assertEquals(
            listOf(
                "https://img.example.com/hero.jpg",
                "https://img.example.com/gallery-1.jpg"
            ),
            event.remoteMediaUrls()
        )
    }
}
