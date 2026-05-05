package com.example.travelcents.data.trip.advisory

import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent

interface TripWeatherContextProvider {
    suspend fun weatherFor(
        event: TravelEvent,
        trip: Itinerary
    ): WeatherContext?
}

interface TripTransportContextProvider {
    suspend fun transportFor(
        event: TravelEvent,
        previousEvent: TravelEvent?,
        trip: Itinerary
    ): TransportContext?
}

interface TripAlternativeProvider {
    suspend fun alternativesFor(
        trip: Itinerary,
        event: TravelEvent,
        reason: AdvisoryReason
    ): List<com.example.travelcents.data.trip.model.EventOption>
}
