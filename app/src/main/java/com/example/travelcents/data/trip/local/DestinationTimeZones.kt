package com.example.travelcents.data.trip.local

import java.time.ZoneId
import java.util.Locale

object DestinationTimeZones {

    fun resolveTimeZoneId(
        destination: String,
        destinationIata: String = ""
    ): String? {
        AirportTimeZones.zoneIdForIataOrNull(destinationIata)?.let { return it.id }

        val normalizedDestination = destination.trim()
        if (normalizedDestination.isBlank()) return null

        val city = normalizeCity(normalizedDestination)
        val country = normalizeCountry(normalizedDestination)
        val destinationKey = normalizeLookupKey(normalizedDestination)
        val cityKey = normalizeLookupKey(city)
        val countryKey = normalizeLookupKey(country)

        DESTINATION_TIME_ZONES[destinationKey]?.let { return it }
        CITY_TIME_ZONES[cityKey]?.let { return it }
        matchAvailableZoneId(cityKey, countryKey)?.let { return it }
        COUNTRY_TIME_ZONES[countryKey]?.let { return it }
        return null
    }

    private fun normalizeCity(destination: String): String {
        val rawCity = destination.substringBefore(",").trim()
        return CITY_ALIASES[normalizeLookupKey(rawCity)] ?: rawCity
    }

    private fun normalizeCountry(destination: String): String {
        val rawCountry = destination.substringAfter(",", "").trim()
        if (rawCountry.isBlank()) return ""
        return COUNTRY_ALIASES[normalizeLookupKey(rawCountry)] ?: rawCountry
    }

    private fun normalizeLookupKey(value: String): String {
        return value
            .lowercase(Locale.US)
            .replace("&", "and")
            .replace(".", " ")
            .replace(",", " ")
            .replace("'", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun matchAvailableZoneId(
        cityKey: String,
        countryKey: String
    ): String? {
        if (cityKey.isBlank()) return null

        val candidates = linkedSetOf<String>()
        availableZoneIdsByCityKey[cityKey]?.let(candidates::addAll)
        if (cityKey.endsWith(" city")) {
            availableZoneIdsByCityKey[cityKey.removeSuffix(" city")]?.let(candidates::addAll)
        }

        if (candidates.size == 1) return candidates.first()

        val countryRegionPrefix = COUNTRY_REGION_PREFIXES[countryKey] ?: return null
        return candidates.firstOrNull { it.startsWith(countryRegionPrefix) }
    }

    private val CANONICAL_ZONE_REGIONS = setOf(
        "Africa",
        "America",
        "Asia",
        "Atlantic",
        "Australia",
        "Europe",
        "Indian",
        "Pacific"
    )

    private val availableZoneIdsByCityKey: Map<String, List<String>> by lazy(LazyThreadSafetyMode.NONE) {
        ZoneId.getAvailableZoneIds()
            .asSequence()
            .filter { zoneId ->
                zoneId.substringBefore("/") in CANONICAL_ZONE_REGIONS
            }
            .groupBy { zoneId ->
                normalizeLookupKey(zoneId.substringAfterLast("/").replace('_', ' '))
            }
    }

    private val COUNTRY_REGION_PREFIXES = mapOf(
        "argentina" to "America/",
        "australia" to "Australia/",
        "austria" to "Europe/",
        "azerbaijan" to "Asia/",
        "bahrain" to "Asia/",
        "bangladesh" to "Asia/",
        "belgium" to "Europe/",
        "brazil" to "America/",
        "bulgaria" to "Europe/",
        "canada" to "America/",
        "cambodia" to "Asia/",
        "chile" to "America/",
        "china" to "Asia/",
        "colombia" to "America/",
        "croatia" to "Europe/",
        "czech republic" to "Europe/",
        "denmark" to "Europe/",
        "egypt" to "Africa/",
        "estonia" to "Europe/",
        "finland" to "Europe/",
        "france" to "Europe/",
        "georgia" to "Asia/",
        "germany" to "Europe/",
        "greece" to "Europe/",
        "hong kong" to "Asia/",
        "hungary" to "Europe/",
        "india" to "Asia/",
        "indonesia" to "Asia/",
        "ireland" to "Europe/",
        "israel" to "Asia/",
        "italy" to "Europe/",
        "japan" to "Asia/",
        "jordan" to "Asia/",
        "kazakhstan" to "Asia/",
        "kenya" to "Africa/",
        "kuwait" to "Asia/",
        "laos" to "Asia/",
        "latvia" to "Europe/",
        "lebanon" to "Asia/",
        "lithuania" to "Europe/",
        "malaysia" to "Asia/",
        "maldives" to "Indian/",
        "mexico" to "America/",
        "morocco" to "Africa/",
        "myanmar" to "Asia/",
        "netherlands" to "Europe/",
        "new zealand" to "Pacific/",
        "norway" to "Europe/",
        "oman" to "Asia/",
        "pakistan" to "Asia/",
        "peru" to "America/",
        "philippines" to "Asia/",
        "poland" to "Europe/",
        "portugal" to "Europe/",
        "qatar" to "Asia/",
        "romania" to "Europe/",
        "russia" to "Europe/",
        "saudi arabia" to "Asia/",
        "serbia" to "Europe/",
        "singapore" to "Asia/",
        "slovakia" to "Europe/",
        "slovenia" to "Europe/",
        "south africa" to "Africa/",
        "south korea" to "Asia/",
        "spain" to "Europe/",
        "sri lanka" to "Asia/",
        "sweden" to "Europe/",
        "switzerland" to "Europe/",
        "taiwan" to "Asia/",
        "thailand" to "Asia/",
        "turkey" to "Europe/",
        "uae" to "Asia/",
        "uk" to "Europe/",
        "united arab emirates" to "Asia/",
        "united kingdom" to "Europe/",
        "usa" to "America/",
        "united states" to "America/",
        "vietnam" to "Asia/"
    )

    private val COUNTRY_ALIASES = mapOf(
        "uae" to "United Arab Emirates",
        "uk" to "United Kingdom",
        "usa" to "United States"
    )

    private val COUNTRY_TIME_ZONES = mapOf(
        "argentina" to "America/Argentina/Buenos_Aires",
        "austria" to "Europe/Vienna",
        "azerbaijan" to "Asia/Baku",
        "bahrain" to "Asia/Bahrain",
        "bangladesh" to "Asia/Dhaka",
        "belgium" to "Europe/Brussels",
        "bulgaria" to "Europe/Sofia",
        "cambodia" to "Asia/Phnom_Penh",
        "chile" to "America/Santiago",
        "china" to "Asia/Shanghai",
        "colombia" to "America/Bogota",
        "croatia" to "Europe/Zagreb",
        "czech republic" to "Europe/Prague",
        "denmark" to "Europe/Copenhagen",
        "egypt" to "Africa/Cairo",
        "estonia" to "Europe/Tallinn",
        "finland" to "Europe/Helsinki",
        "france" to "Europe/Paris",
        "georgia" to "Asia/Tbilisi",
        "germany" to "Europe/Berlin",
        "greece" to "Europe/Athens",
        "hong kong" to "Asia/Hong_Kong",
        "hungary" to "Europe/Budapest",
        "india" to "Asia/Kolkata",
        "ireland" to "Europe/Dublin",
        "israel" to "Asia/Jerusalem",
        "italy" to "Europe/Rome",
        "japan" to "Asia/Tokyo",
        "jordan" to "Asia/Amman",
        "kazakhstan" to "Asia/Almaty",
        "kenya" to "Africa/Nairobi",
        "kuwait" to "Asia/Kuwait",
        "laos" to "Asia/Vientiane",
        "latvia" to "Europe/Riga",
        "lebanon" to "Asia/Beirut",
        "lithuania" to "Europe/Vilnius",
        "malaysia" to "Asia/Kuala_Lumpur",
        "maldives" to "Indian/Maldives",
        "morocco" to "Africa/Casablanca",
        "myanmar" to "Asia/Yangon",
        "netherlands" to "Europe/Amsterdam",
        "new zealand" to "Pacific/Auckland",
        "norway" to "Europe/Oslo",
        "oman" to "Asia/Muscat",
        "pakistan" to "Asia/Karachi",
        "peru" to "America/Lima",
        "philippines" to "Asia/Manila",
        "poland" to "Europe/Warsaw",
        "portugal" to "Europe/Lisbon",
        "qatar" to "Asia/Qatar",
        "romania" to "Europe/Bucharest",
        "saudi arabia" to "Asia/Riyadh",
        "serbia" to "Europe/Belgrade",
        "singapore" to "Asia/Singapore",
        "slovakia" to "Europe/Bratislava",
        "slovenia" to "Europe/Ljubljana",
        "south africa" to "Africa/Johannesburg",
        "south korea" to "Asia/Seoul",
        "spain" to "Europe/Madrid",
        "sri lanka" to "Asia/Colombo",
        "sweden" to "Europe/Stockholm",
        "switzerland" to "Europe/Zurich",
        "taiwan" to "Asia/Taipei",
        "thailand" to "Asia/Bangkok",
        "turkey" to "Europe/Istanbul",
        "united arab emirates" to "Asia/Dubai",
        "united kingdom" to "Europe/London",
        "vietnam" to "Asia/Ho_Chi_Minh"
    )

    private val CITY_ALIASES = mapOf(
        "new york" to "New York City",
        "washington d c" to "Washington DC",
        "washington dc" to "Washington DC"
    )

    private val DESTINATION_TIME_ZONES = mapOf(
        "bali indonesia" to "Asia/Makassar",
        "abu dhabi united arab emirates" to "Asia/Dubai",
        "abu dhabi uae" to "Asia/Dubai",
        "doha qatar" to "Asia/Qatar",
        "washington dc united states" to "America/New_York",
        "washington dc usa" to "America/New_York",
        "washington d c united states" to "America/New_York",
        "washington d c usa" to "America/New_York",
        "hong kong" to "Asia/Hong_Kong",
        "singapore" to "Asia/Singapore"
    )

    private val CITY_TIME_ZONES = mapOf(
        "abu dhabi" to "Asia/Dubai",
        "atlanta" to "America/New_York",
        "bali" to "Asia/Makassar",
        "bangalore" to "Asia/Kolkata",
        "barcelona" to "Europe/Madrid",
        "boston" to "America/New_York",
        "brisbane" to "Australia/Brisbane",
        "cape town" to "Africa/Johannesburg",
        "christchurch" to "Pacific/Auckland",
        "dallas" to "America/Chicago",
        "doha" to "Asia/Qatar",
        "edinburgh" to "Europe/London",
        "florence" to "Europe/Rome",
        "frankfurt" to "Europe/Berlin",
        "geneva" to "Europe/Zurich",
        "hanoi" to "Asia/Ho_Chi_Minh",
        "ho chi minh city" to "Asia/Ho_Chi_Minh",
        "houston" to "America/Chicago",
        "jeddah" to "Asia/Riyadh",
        "johannesburg" to "Africa/Johannesburg",
        "kyoto" to "Asia/Tokyo",
        "kuwait city" to "Asia/Kuwait",
        "lahore" to "Asia/Karachi",
        "las vegas" to "America/Los_Angeles",
        "manama" to "Asia/Bahrain",
        "marrakech" to "Africa/Casablanca",
        "miami" to "America/New_York",
        "milan" to "Europe/Rome",
        "montreal" to "America/Toronto",
        "munich" to "Europe/Berlin",
        "mumbai" to "Asia/Kolkata",
        "nairobi" to "Africa/Nairobi",
        "new delhi" to "Asia/Kolkata",
        "osaka" to "Asia/Tokyo",
        "philadelphia" to "America/New_York",
        "portland" to "America/Los_Angeles",
        "rio de janeiro" to "America/Sao_Paulo",
        "saint petersburg" to "Europe/Moscow",
        "san francisco" to "America/Los_Angeles",
        "seattle" to "America/Los_Angeles",
        "st petersburg" to "Europe/Moscow",
        "tel aviv" to "Asia/Jerusalem",
        "venice" to "Europe/Rome",
        "washington dc" to "America/New_York",
        "wellington" to "Pacific/Auckland"
    )
}
