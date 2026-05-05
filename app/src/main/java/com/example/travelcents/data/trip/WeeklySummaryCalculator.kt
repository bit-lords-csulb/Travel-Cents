package com.example.travelcents.data.trip

import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.BudgetStatus
import com.example.travelcents.data.trip.model.EventSummary
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.WeeklySummary
import com.example.travelcents.data.trip.model.detailValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object WeeklySummaryCalculator {

    fun calculate(itinerary: Itinerary, events: List<TravelEvent>, targetDate: LocalDate = LocalDate.now()): WeeklySummary? {
        if (events.isEmpty()) return null

        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val tripStartDate = runCatching { LocalDate.parse(itinerary.dateFrom, fmt) }.getOrNull() ?: return null
        
        // Find which "week" of the trip we are in
        val daysSinceStart = ChronoUnit.DAYS.between(tripStartDate, targetDate)
        if (daysSinceStart < 0) return null // Trip hasn't started yet
        
        val weekNumber = (daysSinceStart / 7).toInt()
        val weekStartDate = tripStartDate.plusWeeks(weekNumber.toLong())
        val weekEndDate = weekStartDate.plusDays(6)

        val weekEvents = events.filter { event ->
            val eventDate = runCatching { LocalDate.parse(event.date, fmt) }.getOrNull()
            eventDate != null && !eventDate.isBefore(weekStartDate) && !eventDate.isAfter(weekEndDate)
        }

        if (weekEvents.isEmpty()) return null

        val eventSummaries = weekEvents.map { event ->
            EventSummary(
                eventTitle = event.details["title"] ?: event.type.replaceFirstChar { it.uppercase() },
                type = event.type,
                cost = extractCost(event),
                date = event.date
            )
        }

        val totalCost = eventSummaries.sumOf { it.cost }
        val weeklyBudget = if (itinerary.durationDays > 0) {
            (itinerary.budgetTotal / itinerary.durationDays) * 7
        } else {
            0.0
        }

        val budgetStatus = when {
            totalCost > weeklyBudget * 1.1 -> BudgetStatus.OVER_BUDGET
            totalCost < weeklyBudget * 0.9 -> BudgetStatus.UNDER_BUDGET
            else -> BudgetStatus.ON_TRACK
        }

        val interestStats = itinerary.interests.associateWith { interest ->
            weekEvents.count { event ->
                event.details.values.any { it.contains(interest, ignoreCase = true) } ||
                event.type.contains(interest, ignoreCase = true)
            }
        }.filterValues { it > 0 }

        return WeeklySummary(
            tripName = itinerary.tripName,
            weekStartDate = weekStartDate.format(fmt),
            weekEndDate = weekEndDate.format(fmt),
            events = eventSummaries,
            totalCost = totalCost,
            budget = weeklyBudget,
            currency = itinerary.currency,
            interestStats = interestStats,
            budgetStatus = budgetStatus
        )
    }

    private fun extractCost(event: TravelEvent): Double {
        // Try various common detail keys for cost
        val costKeys = listOf("total_price", "rate_per_night", "cost", "amount", "price")
        for (key in costKeys) {
            event.details[key]?.toDoubleOrNull()?.let { return it }
        }
        
        // Fallback for price tier
        return when (event.detailValue(ATTR_PRICE_TIER, "price_tier")) {
            "$" -> 15.0
            "$$" -> 35.0
            "$$$" -> 70.0
            "$$$$" -> 120.0
            else -> 0.0
        }
    }
}
