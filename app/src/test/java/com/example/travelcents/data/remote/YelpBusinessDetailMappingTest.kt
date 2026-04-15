package com.example.travelcents.data.remote

import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_HAS_FOOD_ORDER
import com.example.travelcents.data.trip.model.ATTR_HAS_WAITLIST
import com.example.travelcents.data.trip.model.ATTR_HOURS_RAW
import com.example.travelcents.data.trip.model.ATTR_HOURS_SUMMARY
import com.example.travelcents.data.trip.model.ATTR_LATITUDE
import com.example.travelcents.data.trip.model.ATTR_LONGITUDE
import com.example.travelcents.data.trip.model.ATTR_MENU_URL
import com.example.travelcents.data.trip.model.ATTR_PHONE
import com.example.travelcents.data.trip.model.ATTR_PROFILE_PHOTO_URL
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_PROVIDER
import com.example.travelcents.data.trip.model.ATTR_STATIC_MAP_URL
import com.example.travelcents.data.trip.model.ATTR_YELP_DETAIL_ENRICHED
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.DETAIL_YELP_ID
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.YelpCategory
import com.example.travelcents.data.trip.model.YelpCoordinates
import com.example.travelcents.data.trip.model.YelpLocation
import com.example.travelcents.data.trip.remote.YelpRepository
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YelpBusinessDetailMappingTest {

    @Test
    fun gsonMapping_preservesBasePlanBusinessDetailFields() {
        val json = """
            {
              "id": "biz-123",
              "name": "Moonstone Cafe",
              "image_url": "https://img.example.com/hero.jpg",
              "url": "https://www.yelp.com/biz/moonstone-cafe",
              "rating": 4.7,
              "review_count": 231,
              "phone": "+15625550123",
              "is_closed": false,
              "coordinates": {
                "latitude": 33.7701,
                "longitude": -118.1937
              },
              "categories": [
                { "alias": "cafes", "title": "Cafes" },
                { "alias": "breakfast_brunch", "title": "Breakfast & Brunch" }
              ],
              "location": {
                "display_address": ["123 Ocean Ave", "Long Beach, CA 90802"]
              },
              "hours": [
                {
                  "hours_type": "REGULAR",
                  "is_open_now": true,
                  "open": [
                    { "day": 1, "start": "0900", "end": "1700", "is_overnight": false }
                  ]
                }
              ],
              "photos": [
                "https://img.example.com/photo-1.jpg",
                "https://img.example.com/photo-2.jpg"
              ],
              "attributes": {
                "business_url": "https://www.yelp.com/biz/moonstone-cafe",
                "menu_url": "https://menu.example.com/moonstone",
                "waitlist_reservation": true,
                "food_ordering": false
              }
            }
        """.trimIndent()

        val business = Gson().fromJson(json, YelpBusiness::class.java)

        assertEquals("biz-123", business.id)
        assertEquals("Moonstone Cafe", business.name)
        assertEquals("https://img.example.com/hero.jpg", business.imageUrl)
        assertEquals(4.7, business.rating, 0.0)
        assertEquals(231, business.reviewCount)
        assertEquals("+15625550123", business.phone)
        assertFalse(business.isClosed)
        assertEquals(33.7701, business.coordinates?.latitude ?: 0.0, 0.0)
        assertEquals(-118.1937, business.coordinates?.longitude ?: 0.0, 0.0)
        assertEquals(
            listOf("123 Ocean Ave", "Long Beach, CA 90802"),
            business.location?.displayAddress
        )
        assertEquals(listOf("Cafes", "Breakfast & Brunch"), business.categories.map { it.title })
        assertEquals(listOf("https://img.example.com/photo-1.jpg", "https://img.example.com/photo-2.jpg"), business.photos)
        assertEquals(1, business.hours?.firstOrNull()?.open?.size ?: 0)
        assertNotNull(business.attributes)
        assertEquals("https://menu.example.com/moonstone", business.attributes?.get("menu_url"))
        assertEquals(true, business.attributes?.get("waitlist_reservation"))
        assertEquals("https://www.yelp.com/biz/moonstone-cafe", business.url)
    }

    @Test
    fun businessDetailAttributes_mapsCanonicalPhase1Keys() {
        val business = Gson().fromJson(
            """
                {
                  "id": "biz-123",
                  "name": "Moonstone Cafe",
                  "image_url": "https://img.example.com/hero.jpg",
                  "url": "https://www.yelp.com/biz/moonstone-cafe",
                  "rating": 4.7,
                  "review_count": 231,
                  "phone": "+15625550123",
                  "is_closed": false,
                  "coordinates": {
                    "latitude": 33.7701,
                    "longitude": -118.1937
                  },
                  "categories": [
                    { "alias": "cafes", "title": "Cafes" },
                    { "alias": "breakfast_brunch", "title": "Breakfast & Brunch" }
                  ],
                  "location": {
                    "display_address": ["123 Ocean Ave", "Long Beach, CA 90802"]
                  },
                  "hours": [
                    {
                      "hours_type": "REGULAR",
                      "is_open_now": true,
                      "open": [
                        { "day": 1, "start": "0900", "end": "1700", "is_overnight": false }
                      ]
                    }
                  ],
                  "attributes": {
                    "menu_url": "https://menu.example.com/moonstone",
                    "waitlist_reservation": true,
                    "food_ordering": false
                  }
                }
            """.trimIndent(),
            YelpBusiness::class.java
        )

        val attrs = YelpRepository.businessDetailAttributes(business)

        assertEquals("biz-123", attrs[DETAIL_YELP_ID])
        assertEquals("Moonstone Cafe", attrs[ATTR_BUSINESS_NAME])
        assertEquals("123 Ocean Ave, Long Beach, CA 90802", attrs[ATTR_BUSINESS_ADDRESS])
        assertEquals("Cafes, Breakfast & Brunch", attrs[ATTR_CATEGORIES])
        assertEquals("+15625550123", attrs[ATTR_PHONE])
        assertEquals("4.7", attrs[ATTR_AVERAGE_RATING])
        assertEquals("231", attrs[ATTR_REVIEW_COUNT])
        assertEquals("https://www.yelp.com/biz/moonstone-cafe", attrs[ATTR_YELP_URL])
        assertEquals("https://img.example.com/hero.jpg", attrs[ATTR_PROFILE_PHOTO_URL])
        assertEquals("https://menu.example.com/moonstone", attrs[ATTR_MENU_URL])
        assertEquals("true", attrs[ATTR_HAS_WAITLIST])
        assertEquals("false", attrs[ATTR_HAS_FOOD_ORDER])
        assertEquals("33.7701", attrs[ATTR_LATITUDE])
        assertEquals("-118.1937", attrs[ATTR_LONGITUDE])
        assertEquals("osm_staticmap", attrs[ATTR_STATIC_MAP_PROVIDER])
        assertEquals("Tue 0900-1700", attrs[ATTR_HOURS_SUMMARY])
        assertEquals("1,0900,1700,false", attrs[ATTR_HOURS_RAW])
        assertNotNull(attrs[ATTR_STATIC_MAP_URL])
    }

    @Test
    fun applyBusinessDetailEnrichment_updatesSelectedEventAndMatchingOption() {
        val detail = YelpBusiness(
            id = "biz-123",
            name = "Moonstone Cafe",
            imageUrl = "https://img.example.com/hero-updated.jpg",
            url = "https://www.yelp.com/biz/moonstone-cafe",
            rating = 4.7,
            reviewCount = 231,
            phone = "+15625550123",
            categories = listOf(YelpCategory(alias = "cafes", title = "Cafes")),
            location = YelpLocation(displayAddress = listOf("123 Ocean Ave", "Long Beach, CA 90802")),
            coordinates = YelpCoordinates(latitude = 33.7701, longitude = -118.1937),
            photos = listOf(
                "https://img.example.com/photo-1.jpg",
                "https://img.example.com/photo-2.jpg"
            )
        )
        val event = TravelEvent(
            eventId = "event-1",
            type = "restaurant",
            itineraryId = "trip-1",
            imageUrl = "https://img.example.com/old.jpg",
            details = mapOf(
                DETAIL_YELP_ID to "biz-123",
                ATTR_BUSINESS_NAME to "Moonstone Cafe"
            )
        )
        val options = listOf(
            EventOption(
                optionId = "opt-1",
                eventId = "event-1",
                selected = true,
                details = mapOf(
                    DETAIL_YELP_ID to "biz-123",
                    ATTR_BUSINESS_NAME to "Moonstone Cafe"
                )
            ),
            EventOption(
                optionId = "opt-2",
                eventId = "event-1",
                selected = false,
                details = mapOf(
                    DETAIL_YELP_ID to "biz-999",
                    ATTR_BUSINESS_NAME to "Backup Place"
                )
            )
        )

        val result = YelpRepository.applyBusinessDetailEnrichment(event, options, detail)

        assertNotNull(result)
        assertEquals("biz-123", result?.yelpId)
        assertEquals("true", result?.event?.details?.get(ATTR_YELP_DETAIL_ENRICHED))
        assertEquals("https://img.example.com/hero-updated.jpg", result?.event?.imageUrl)
        assertEquals(
            listOf("https://img.example.com/photo-1.jpg", "https://img.example.com/photo-2.jpg"),
            result?.event?.photoUrls
        )
        assertEquals(setOf("opt-1"), result?.updatedOptionIds)
        assertEquals("true", result?.options?.first()?.details?.get(ATTR_YELP_DETAIL_ENRICHED))
        assertEquals(
            listOf("https://img.example.com/photo-1.jpg", "https://img.example.com/photo-2.jpg"),
            result?.options?.first()?.photoUrls
        )
        assertEquals("Backup Place", result?.options?.get(1)?.details?.get(ATTR_BUSINESS_NAME))
        assertFalse(result?.options?.get(1)?.details?.containsKey(ATTR_YELP_DETAIL_ENRICHED) == true)
    }

    @Test
    fun needsEventEnrichment_returnsFalseAfterMarkerIsPersistedOnEventAndSelectedOption() {
        val event = TravelEvent(
            eventId = "event-1",
            type = "restaurant",
            itineraryId = "trip-1",
            details = mapOf(
                DETAIL_YELP_ID to "biz-123",
                ATTR_YELP_DETAIL_ENRICHED to "true"
            )
        )
        val options = listOf(
            EventOption(
                optionId = "opt-1",
                eventId = "event-1",
                selected = true,
                details = mapOf(
                    DETAIL_YELP_ID to "biz-123",
                    ATTR_YELP_DETAIL_ENRICHED to "true"
                )
            )
        )

        assertFalse(YelpRepository.needsEventEnrichment(event, options))
        assertTrue(YelpRepository.needsEventEnrichment(event.copy(details = mapOf(DETAIL_YELP_ID to "biz-123")), options))
    }
}
