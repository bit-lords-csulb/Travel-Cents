package com.example.travelcents.data.trip.remote

import com.example.travelcents.data.trip.model.ATTR_AIRLINE_BOOKING_URL
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class FlightOffer(
    val source: String,
    val subtitle: String,
    val link: String,
    val logoUrl: String?
)

object FlightOffersResolver {

    fun resolve(event: TravelEvent, adults: Int): List<FlightOffer> {
        val from = event.detailValue("origin_airport")?.uppercase(Locale.US)?.takeIf { it.length in 3..4 }
        val to = event.detailValue("destination_airport")?.uppercase(Locale.US)?.takeIf { it.length in 3..4 }
        val date = event.date.takeIf { it.isNotBlank() } ?: return emptyList()
        if (from == null || to == null) return emptyList()
        val safeAdults = adults.coerceAtLeast(1)

        val offers = mutableListOf<FlightOffer>()
        skyscanner(from, to, date, safeAdults)?.let(offers::add)
        kayak(from, to, date, safeAdults)?.let(offers::add)
        googleFlights(from, to, date)?.let(offers::add)
        kiwi(from, to, date)?.let(offers::add)
        airlineDirect(event)?.let(offers::add)
        return offers
    }

    private fun skyscanner(from: String, to: String, isoDate: String, adults: Int): FlightOffer? {
        val yymmdd = toCompactDate(isoDate) ?: return null
        val url = "https://www.skyscanner.com/transport/flights/${from.lowercase(Locale.US)}/${to.lowercase(Locale.US)}/$yymmdd/?adults=$adults"
        return FlightOffer(
            source = "Skyscanner",
            subtitle = "Compare across airlines",
            link = url,
            logoUrl = faviconFor("skyscanner.com")
        )
    }

    private fun kayak(from: String, to: String, isoDate: String, adults: Int): FlightOffer? {
        if (!isoDateLooksValid(isoDate)) return null
        val url = "https://www.kayak.com/flights/$from-$to/$isoDate/${adults}adults"
        return FlightOffer(
            source = "Kayak",
            subtitle = "Search prices and itineraries",
            link = url,
            logoUrl = faviconFor("kayak.com")
        )
    }

    private fun googleFlights(from: String, to: String, isoDate: String): FlightOffer? {
        if (!isoDateLooksValid(isoDate)) return null
        val q = "Flights from $from to $to on $isoDate".replace(' ', '+')
        val url = "https://www.google.com/travel/flights?q=$q"
        return FlightOffer(
            source = "Google Flights",
            subtitle = "Open in Google Flights",
            link = url,
            logoUrl = faviconFor("google.com")
        )
    }

    private fun kiwi(from: String, to: String, isoDate: String): FlightOffer? {
        if (!isoDateLooksValid(isoDate)) return null
        val url = "https://www.kiwi.com/en/search/results/$from/$to/$isoDate/no-return"
        return FlightOffer(
            source = "Kiwi.com",
            subtitle = "Hidden-city and combined fares",
            link = url,
            logoUrl = faviconFor("kiwi.com")
        )
    }

    private fun airlineDirect(event: TravelEvent): FlightOffer? {
        val url = event.detailValue(ATTR_AIRLINE_BOOKING_URL)?.takeIf { it.isNotBlank() } ?: return null
        val airline = event.detailValue("airline")?.takeIf { it.isNotBlank() } ?: "Airline"
        return FlightOffer(
            source = airline,
            subtitle = "Book directly with the airline",
            link = url,
            logoUrl = null
        )
    }

    private fun toCompactDate(isoDate: String): String? {
        return try {
            LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
                .format(DateTimeFormatter.ofPattern("yyMMdd"))
        } catch (_: Exception) {
            null
        }
    }

    private fun isoDateLooksValid(isoDate: String): Boolean {
        return try {
            LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun faviconFor(domain: String): String =
        "https://www.google.com/s2/favicons?domain=$domain&sz=64"
}
