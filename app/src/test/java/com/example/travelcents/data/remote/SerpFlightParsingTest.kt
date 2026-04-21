package com.example.travelcents.data.remote

import com.example.travelcents.data.trip.model.SerpFlightResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class SerpFlightParsingTest {

    private val gson = Gson()

    @Test
    fun `deserializes live flight timestamps from airport nodes`() {
        val json = """
            {
              "best_flights": [
                {
                  "price": 412,
                  "total_duration": 337,
                  "departure_token": "token-123",
                  "type": "Round trip",
                  "flights": [
                    {
                      "airline": "American",
                      "flight_number": "AA 1",
                      "duration": 337,
                      "departure_airport": {
                        "id": "LAX",
                        "name": "Los Angeles International Airport",
                        "time": "2026-08-01 06:00"
                      },
                      "arrival_airport": {
                        "id": "JFK",
                        "name": "John F. Kennedy International Airport",
                        "time": "2026-08-01 14:37"
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, SerpFlightResponse::class.java)
        val option = response.bestFlights.orEmpty().first()
        val leg = response.bestFlights.orEmpty().first().flights.first()

        assertEquals("token-123", option.departureToken)
        assertEquals("Round trip", option.type)
        assertEquals("2026-08-01 06:00", leg.departureAirport.time)
        assertEquals("2026-08-01 14:37", leg.arrivalAirport.time)
    }

    @Test
    fun `still deserializes legacy flat flight timestamps`() {
        val json = """
            {
              "best_flights": [
                {
                  "price": 412,
                  "total_duration": 337,
                  "flights": [
                    {
                      "airline": "American",
                      "flight_number": "AA 1",
                      "duration": 337,
                      "departure_time": "2026-08-01 06:00",
                      "arrival_time": "2026-08-01 14:37",
                      "departure_airport": {
                        "id": "LAX",
                        "name": "Los Angeles International Airport"
                      },
                      "arrival_airport": {
                        "id": "JFK",
                        "name": "John F. Kennedy International Airport"
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, SerpFlightResponse::class.java)
        val leg = response.bestFlights.orEmpty().first().flights.first()

        assertEquals("2026-08-01 06:00", leg.departureTime)
        assertEquals("2026-08-01 14:37", leg.arrivalTime)
    }
}
