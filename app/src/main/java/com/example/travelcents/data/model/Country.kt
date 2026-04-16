package com.example.travelcents.data.model

data class Country(
    val name: String,
    val code: String,
    val currencyCode: String,
    val flag: String = "" // Could add flag emojis or icons later
)

val majorCountries = listOf(
    Country("United States", "US", "USD"),
    Country("United Kingdom", "GB", "GBP"),
    Country("Canada", "CA", "CAD"),
    Country("Australia", "AU", "AUD"),
    Country("Germany", "DE", "EUR"),
    Country("France", "FR", "EUR"),
    Country("Japan", "JP", "JPY"),
    Country("China", "CN", "CNY"),
    Country("India", "IN", "INR"),
    Country("Brazil", "BR", "BRL"),
    Country("Mexico", "MX", "MXN"),
    Country("South Korea", "KR", "KRW"),
    Country("Italy", "IT", "EUR"),
    Country("Spain", "ES", "EUR"),
    Country("Singapore", "SG", "SGD")
)
