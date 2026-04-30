package com.example.travelcents.ui.main.chats.voting

import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.Event
import com.example.travelcents.data.trip.model.Itinerary
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupEventTravelEventMapperTest {

    @Test
    fun mapsYelpRestaurantProposalToLinkedTripTravelEvent() {
        val event = Event(
            id = "group-event-1",
            title = "Sushi Nakazawa",
            description = "Omakase dinner",
            location = "23 Commerce St, New York, NY",
            date = "2026-06-04",
            startTime = "7:30 PM",
            endTime = "9:00 PM",
            photoUrl = "https://img.example.com/sushi.jpg",
            createdBy = "user-2",
            createdByName = "Dana",
            upvotes = listOf("user-2", "user-3"),
            yelpId = "yelp-1",
            yelpUrl = "https://www.yelp.com/biz/sushi-nakazawa",
            yelpCategory = "Sushi Bars",
            yelpCategories = listOf("Sushi Bars", "Japanese"),
            yelpRating = 4.6,
            yelpReviewCount = 1200,
            yelpImageUrl = "https://img.example.com/yelp-sushi.jpg"
        )

        val mapped = event.toLinkedTripTravelEvent(
            group = linkedGroup(),
            linkedTrip = linkedTrip()
        )

        assertEquals("group-event-1", mapped.eventId)
        assertEquals("restaurant", mapped.type)
        assertEquals("trip-1", mapped.itineraryId)
        assertEquals("America/New_York", mapped.tz)
        assertEquals("2026-06-04", mapped.date)
        assertEquals("19:30", mapped.startTime)
        assertEquals("21:00", mapped.endTime)
        assertEquals("https://img.example.com/sushi.jpg", mapped.imageUrl)
        assertEquals("yelp-1", mapped.selectedOptionId)
        assertEquals("Sushi Nakazawa", mapped.details[ATTR_BUSINESS_NAME])
        assertEquals("Sushi Nakazawa", mapped.details["restaurant_name"])
        assertEquals("23 Commerce St, New York, NY", mapped.details[ATTR_BUSINESS_ADDRESS])
        assertEquals("Sushi Bars, Japanese", mapped.details[ATTR_CATEGORIES])
        assertEquals("yelp-1", mapped.details[DETAIL_YELP_ID])
        assertEquals("https://www.yelp.com/biz/sushi-nakazawa", mapped.details[ATTR_YELP_URL])
        assertEquals("4.6", mapped.details[ATTR_AVERAGE_RATING])
        assertEquals("1200", mapped.details[ATTR_REVIEW_COUNT])
        assertEquals("group-1", mapped.details["group_id"])
        assertEquals("2", mapped.details["group_upvote_count"])
    }

    @Test
    fun mapsCustomProposalToActivityWhenNoYelpMetadataExists() {
        val event = Event(
            id = "group-event-2",
            title = "Sunset walk",
            description = "Walk the waterfront",
            location = "Brooklyn Bridge Park",
            date = "",
            startTime = "18:00",
            photoUrl = "https://img.example.com/walk.jpg"
        )

        val mapped = event.toLinkedTripTravelEvent(
            group = linkedGroup(),
            linkedTrip = linkedTrip()
        )

        assertEquals("activity", mapped.type)
        assertEquals("trip-1", mapped.itineraryId)
        assertEquals("", mapped.date)
        assertEquals("18:00", mapped.startTime)
        assertEquals("", mapped.endTime)
        assertEquals("Sunset walk", mapped.details["activity_name"])
        assertEquals("group_chat", mapped.details["source"])
    }

    private fun linkedGroup(): Group {
        return Group(
            id = "group-1",
            name = "NYC Crew",
            linkedTripId = "trip-1",
            linkedTripOwnerId = "owner-1"
        )
    }

    private fun linkedTrip(): Itinerary {
        return Itinerary(
            itineraryId = "trip-1",
            userId = "owner-1",
            tripName = "NYC Weekend",
            destination = "New York, USA",
            origin = "Los Angeles, USA",
            timeZoneId = "America/New_York",
            dateFrom = "2026-06-03",
            dateTo = "2026-06-07",
            durationDays = 5,
            currency = "USD",
            travelStyle = "comfort",
            adults = 2,
            children = 0,
            createdAt = "2026-01-01T00:00:00Z",
            status = "active",
            eventIds = emptyList(),
            ownerUid = "owner-1"
        )
    }
}
