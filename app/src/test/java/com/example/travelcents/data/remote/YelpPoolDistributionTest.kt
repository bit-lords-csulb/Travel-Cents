package com.example.travelcents.data.remote

import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.YelpCategory
import com.example.travelcents.data.trip.model.YelpLocation
import com.example.travelcents.data.trip.remote.YelpRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YelpPoolDistributionTest {

    @Test
    fun distributePoolToEvents_forSevenDaysPreservesAllLoadedBusinessesAsOptions() {
        val pool = (0 until 35).map(::business)
        val dates = (1..7).map { "2026-06-0$it" }

        val events = YelpRepository.distributePoolToEvents(
            pool = pool,
            dates = dates,
            type = "restaurant",
            itineraryId = "trip-1"
        )

        val usedBusinessIds = events
            .flatMap { event -> event.options.mapNotNull { it.details["yelp_id"] } }
            .toSet()
        val selectedBusinessIds = events
            .mapNotNull { event -> event.options.firstOrNull { it.selected }?.details?.get("yelp_id") }
            .toSet()

        assertEquals(7, events.size)
        assertTrue(events.all { it.options.size == 35 })
        assertEquals(35, usedBusinessIds.size)
        assertEquals((0 until 35).map { "biz-$it" }.toSet(), usedBusinessIds)
        assertEquals(setOf("biz-0", "biz-5", "biz-10", "biz-15", "biz-20", "biz-25", "biz-30"), selectedBusinessIds)
    }

    @Test
    fun distributePoolToEvents_ordersEachDayByItsPreferredChunkBeforeGlobalBackups() {
        val pool = (0 until 12).map(::business)
        val dates = listOf("2026-06-01", "2026-06-02", "2026-06-03")

        val events = YelpRepository.distributePoolToEvents(
            pool = pool,
            dates = dates,
            type = "restaurant",
            itineraryId = "trip-1"
        )

        val dayTwoOptionIds = events[1].options.mapNotNull { it.details["yelp_id"] }

        assertEquals(listOf("biz-5", "biz-6", "biz-7", "biz-8", "biz-9"), dayTwoOptionIds.take(5))
        assertEquals(12, dayTwoOptionIds.size)
    }

    @Test
    fun distributePoolToEvents_keepsAllLoadedBusinessesForSmallPoolsAndSkipsDaysBeyondPoolSize() {
        val pool = (0 until 3).map(::business)
        val dates = listOf("2026-06-01", "2026-06-02", "2026-06-03", "2026-06-04", "2026-06-05")

        val events = YelpRepository.distributePoolToEvents(
            pool = pool,
            dates = dates,
            type = "activity",
            itineraryId = "trip-1"
        )

        assertEquals(3, events.size)
        assertTrue(events.all { it.options.size == 3 })
        assertTrue(events.all { event ->
            event.options.mapNotNull { it.details["yelp_id"] }.toSet() == setOf("biz-0", "biz-1", "biz-2")
        })
        assertEquals(listOf("2026-06-01", "2026-06-02", "2026-06-03"), events.map { it.date })
    }

    private fun business(index: Int) = YelpBusiness(
        id = "biz-$index",
        name = "Business $index",
        imageUrl = "https://img.example.com/$index.jpg",
        rating = 4.0 + (index % 5) * 0.1,
        reviewCount = 100 + index,
        categories = listOf(YelpCategory(alias = "restaurants", title = "Restaurants")),
        location = YelpLocation(displayAddress = listOf("$index Main St", "Long Beach, CA")),
        phone = "+15625550${index.toString().padStart(3, '0')}"
    )
}
