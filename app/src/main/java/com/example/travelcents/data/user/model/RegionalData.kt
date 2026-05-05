package com.example.travelcents.data.user.model

data class CountryInfo(
    val name: String,
    val regions: List<RegionInfo>,
    val defaultCurrency: String,
    val usesFahrenheit: Boolean = false,
    val dateFormat: String = "MM/dd/yyyy",
    val timeFormat: String = "h:mm a"
)

data class RegionInfo(
    val name: String,
    val cities: List<CityInfo>
)

data class CityInfo(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
)

object RegionalData {
    val countries = listOf(
        CountryInfo(
            name = "United States",
            usesFahrenheit = true,
            defaultCurrency = "USD",
            dateFormat = "MM/dd/yyyy",
            timeFormat = "h:mm a",
            regions = listOf(
                RegionInfo(
                    name = "California",
                    cities = listOf(
                        CityInfo("Long Beach", 33.7701, -118.1937, "America/Los_Angeles"),
                        CityInfo("Los Angeles", 34.0522, -118.2437, "America/Los_Angeles"),
                        CityInfo("San Francisco", 37.7749, -122.4194, "America/Los_Angeles"),
                        CityInfo("San Diego", 32.7157, -117.1611, "America/Los_Angeles"),
                        CityInfo("Sacramento", 38.5816, -121.4944, "America/Los_Angeles")
                    )
                ),
                RegionInfo(
                    name = "New York",
                    cities = listOf(
                        CityInfo("New York City", 40.7128, -74.0060, "America/New_York"),
                        CityInfo("Buffalo", 42.8864, -78.8784, "America/New_York"),
                        CityInfo("Albany", 42.6526, -73.7562, "America/New_York")
                    )
                ),
                RegionInfo(
                    name = "Texas",
                    cities = listOf(
                        CityInfo("Austin", 30.2672, -97.7431, "America/Chicago"),
                        CityInfo("Houston", 29.7604, -95.3698, "America/Chicago"),
                        CityInfo("Dallas", 32.7767, -96.7970, "America/Chicago"),
                        CityInfo("San Antonio", 29.4241, -98.4936, "America/Chicago")
                    )
                ),
                RegionInfo(
                    name = "Florida",
                    cities = listOf(
                        CityInfo("Miami", 25.7617, -80.1918, "America/New_York"),
                        CityInfo("Orlando", 28.5383, -81.3792, "America/New_York"),
                        CityInfo("Tampa", 27.9506, -82.4572, "America/New_York")
                    )
                ),
                RegionInfo(
                    name = "Illinois",
                    cities = listOf(CityInfo("Chicago", 41.8781, -87.6298, "America/Chicago"))
                )
            )
        ),
        CountryInfo(
            name = "Canada",
            defaultCurrency = "CAD",
            dateFormat = "yyyy-MM-dd",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Ontario",
                    cities = listOf(
                        CityInfo("Toronto", 43.6532, -79.3832, "America/Toronto"),
                        CityInfo("Ottawa", 45.4215, -75.6972, "America/Toronto")
                    )
                ),
                RegionInfo(
                    name = "Quebec",
                    cities = listOf(
                        CityInfo("Montreal", 45.5017, -73.5673, "America/Montreal"),
                        CityInfo("Quebec City", 46.8139, -71.2080, "America/Montreal")
                    )
                ),
                RegionInfo(
                    name = "British Columbia",
                    cities = listOf(
                        CityInfo("Vancouver", 49.2827, -123.1207, "America/Vancouver"),
                        CityInfo("Victoria", 48.4284, -123.3656, "America/Vancouver")
                    )
                )
            )
        ),
        CountryInfo(
            name = "United Kingdom",
            defaultCurrency = "GBP",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "England",
                    cities = listOf(
                        CityInfo("London", 51.5074, -0.1278, "Europe/London"),
                        CityInfo("Manchester", 53.4808, -2.2426, "Europe/London"),
                        CityInfo("Birmingham", 52.4862, -1.8904, "Europe/London"),
                        CityInfo("Liverpool", 53.4084, -2.9916, "Europe/London")
                    )
                ),
                RegionInfo(
                    name = "Scotland",
                    cities = listOf(
                        CityInfo("Edinburgh", 55.9533, -3.1883, "Europe/London"),
                        CityInfo("Glasgow", 55.8642, -4.2518, "Europe/London")
                    )
                )
            )
        ),
        CountryInfo(
            name = "Australia",
            defaultCurrency = "AUD",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "h:mm a",
            regions = listOf(
                RegionInfo(
                    name = "New South Wales",
                    cities = listOf(CityInfo("Sydney", -33.8688, 151.2093, "Australia/Sydney"))
                ),
                RegionInfo(
                    name = "Victoria",
                    cities = listOf(CityInfo("Melbourne", -37.8136, 144.9631, "Australia/Melbourne"))
                ),
                RegionInfo(
                    name = "Queensland",
                    cities = listOf(CityInfo("Brisbane", -27.4698, 153.0251, "Australia/Brisbane"))
                )
            )
        ),
        CountryInfo(
            name = "Germany",
            defaultCurrency = "EUR",
            dateFormat = "dd.MM.yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Berlin",
                    cities = listOf(CityInfo("Berlin", 52.52, 13.405, "Europe/Berlin"))
                ),
                RegionInfo(
                    name = "Bavaria",
                    cities = listOf(CityInfo("Munich", 48.1351, 11.582, "Europe/Berlin"))
                ),
                RegionInfo(
                    name = "Hamburg",
                    cities = listOf(CityInfo("Hamburg", 53.5511, 9.9937, "Europe/Berlin"))
                )
            )
        ),
        CountryInfo(
            name = "France",
            defaultCurrency = "EUR",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Île-de-France",
                    cities = listOf(CityInfo("Paris", 48.8566, 2.3522, "Europe/Paris"))
                ),
                RegionInfo(
                    name = "Provence-Alpes-Côte d'Azur",
                    cities = listOf(CityInfo("Marseille", 43.2965, 5.3698, "Europe/Paris"))
                )
            )
        ),
        CountryInfo(
            name = "Italy",
            defaultCurrency = "EUR",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Lazio",
                    cities = listOf(CityInfo("Rome", 41.9028, 12.4964, "Europe/Rome"))
                ),
                RegionInfo(
                    name = "Lombardy",
                    cities = listOf(CityInfo("Milan", 45.4642, 9.19, "Europe/Rome"))
                ),
                RegionInfo(
                    name = "Veneto",
                    cities = listOf(CityInfo("Venice", 45.4408, 12.3155, "Europe/Rome"))
                )
            )
        ),
        CountryInfo(
            name = "Japan",
            defaultCurrency = "JPY",
            dateFormat = "yyyy/MM/dd",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Tokyo",
                    cities = listOf(CityInfo("Tokyo", 35.6895, 139.6917, "Asia/Tokyo"))
                ),
                RegionInfo(
                    name = "Osaka",
                    cities = listOf(CityInfo("Osaka", 34.6937, 135.5023, "Asia/Tokyo"))
                ),
                RegionInfo(
                    name = "Kyoto",
                    cities = listOf(CityInfo("Kyoto", 35.0116, 135.7681, "Asia/Tokyo"))
                )
            )
        ),
        CountryInfo(
            name = "India",
            defaultCurrency = "INR",
            dateFormat = "dd-MM-yyyy",
            timeFormat = "h:mm a",
            regions = listOf(
                RegionInfo(
                    name = "Maharashtra",
                    cities = listOf(
                        CityInfo("Mumbai", 19.076, 72.8777, "Asia/Kolkata"),
                        CityInfo("Pune", 18.5204, 73.8567, "Asia/Kolkata")
                    )
                ),
                RegionInfo(
                    name = "Delhi",
                    cities = listOf(CityInfo("New Delhi", 28.6139, 77.209, "Asia/Kolkata"))
                ),
                RegionInfo(
                    name = "Karnataka",
                    cities = listOf(CityInfo("Bangalore", 12.9716, 77.5946, "Asia/Kolkata"))
                )
            )
        ),
        CountryInfo(
            name = "China",
            defaultCurrency = "CNY",
            dateFormat = "yyyy-MM-dd",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Beijing",
                    cities = listOf(CityInfo("Beijing", 39.9042, 116.4074, "Asia/Shanghai"))
                ),
                RegionInfo(
                    name = "Shanghai",
                    cities = listOf(CityInfo("Shanghai", 31.2304, 121.4737, "Asia/Shanghai"))
                ),
                RegionInfo(
                    name = "Guangdong",
                    cities = listOf(CityInfo("Guangzhou", 23.1291, 113.2644, "Asia/Shanghai"))
                )
            )
        ),
        CountryInfo(
            name = "Brazil",
            defaultCurrency = "BRL",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "São Paulo",
                    cities = listOf(CityInfo("São Paulo", -23.5505, -46.6333, "America/Sao_Paulo"))
                ),
                RegionInfo(
                    name = "Rio de Janeiro",
                    cities = listOf(CityInfo("Rio de Janeiro", -22.9068, -43.1729, "America/Sao_Paulo"))
                )
            )
        ),
        CountryInfo(
            name = "Mexico",
            defaultCurrency = "MXN",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "CDMX",
                    cities = listOf(CityInfo("Mexico City", 19.4326, -99.1332, "America/Mexico_City"))
                ),
                RegionInfo(
                    name = "Jalisco",
                    cities = listOf(CityInfo("Guadalajara", 20.6597, -103.3496, "America/Mexico_City"))
                )
            )
        ),
        CountryInfo(
            name = "Spain",
            defaultCurrency = "EUR",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Madrid",
                    cities = listOf(CityInfo("Madrid", 40.4168, -3.7038, "Europe/Madrid"))
                ),
                RegionInfo(
                    name = "Catalonia",
                    cities = listOf(CityInfo("Barcelona", 41.3851, 2.1734, "Europe/Madrid"))
                )
            )
        ),
        CountryInfo(
            name = "Singapore",
            defaultCurrency = "SGD",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Singapore",
                    cities = listOf(CityInfo("Singapore", 1.3521, 103.8198, "Asia/Singapore"))
                )
            )
        ),
        CountryInfo(
            name = "South Korea",
            defaultCurrency = "KRW",
            dateFormat = "yyyy. MM. dd.",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Seoul",
                    cities = listOf(CityInfo("Seoul", 37.5665, 126.9780, "Asia/Seoul"))
                )
            )
        ),
        CountryInfo(
            name = "South Africa",
            defaultCurrency = "ZAR",
            dateFormat = "yyyy/MM/dd",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Western Cape",
                    cities = listOf(CityInfo("Cape Town", -33.9249, 18.4241, "Africa/Johannesburg"))
                ),
                RegionInfo(
                    name = "Gauteng",
                    cities = listOf(CityInfo("Johannesburg", -26.2041, 28.0473, "Africa/Johannesburg"))
                )
            )
        ),
        CountryInfo(
            name = "United Arab Emirates",
            defaultCurrency = "AED",
            dateFormat = "dd/MM/yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Dubai",
                    cities = listOf(CityInfo("Dubai", 25.2048, 55.2708, "Asia/Dubai"))
                ),
                RegionInfo(
                    name = "Abu Dhabi",
                    cities = listOf(CityInfo("Abu Dhabi", 24.4539, 54.3773, "Asia/Dubai"))
                )
            )
        ),
        CountryInfo(
            name = "Russia",
            defaultCurrency = "RUB",
            dateFormat = "dd.MM.yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Moscow",
                    cities = listOf(CityInfo("Moscow", 55.7558, 37.6173, "Europe/Moscow"))
                ),
                RegionInfo(
                    name = "Saint Petersburg",
                    cities = listOf(CityInfo("Saint Petersburg", 59.9343, 30.3351, "Europe/Moscow"))
                )
            )
        ),
        CountryInfo(
            name = "Turkey",
            defaultCurrency = "TRY",
            dateFormat = "dd.MM.yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Istanbul",
                    cities = listOf(CityInfo("Istanbul", 41.0082, 28.9784, "Europe/Istanbul"))
                ),
                RegionInfo(
                    name = "Ankara",
                    cities = listOf(CityInfo("Ankara", 39.9334, 32.8597, "Europe/Istanbul"))
                )
            )
        ),
        CountryInfo(
            name = "Netherlands",
            defaultCurrency = "EUR",
            dateFormat = "dd-MM-yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "North Holland",
                    cities = listOf(CityInfo("Amsterdam", 52.3676, 4.9041, "Europe/Amsterdam"))
                )
            )
        ),
        CountryInfo(
            name = "Switzerland",
            defaultCurrency = "CHF",
            dateFormat = "dd.MM.yyyy",
            timeFormat = "HH:mm",
            regions = listOf(
                RegionInfo(
                    name = "Zurich",
                    cities = listOf(CityInfo("Zurich", 47.3769, 8.5417, "Europe/Zurich"))
                ),
                RegionInfo(
                    name = "Geneva",
                    cities = listOf(CityInfo("Geneva", 46.2044, 6.1432, "Europe/Zurich"))
                )
            )
        )
    ).sortedBy { it.name }

    fun getCountry(name: String) = countries.find { it.name == name }
}
