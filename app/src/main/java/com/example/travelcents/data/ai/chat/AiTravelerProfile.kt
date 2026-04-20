package com.example.travelcents.data.ai.chat

data class AiTravelerProfile(
    val destination: String = "",
    val origin: String = "",
    val dateWindow: String = "",
    val budgetSummary: String = "",
    val partySummary: String = "",
    val travelPace: String = "",
    val interests: List<String> = emptyList(),
    val cuisinePreferences: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val notes: List<String> = emptyList()
) {
    val hasSignals: Boolean
        get() = destination.isNotBlank() ||
            origin.isNotBlank() ||
            dateWindow.isNotBlank() ||
            budgetSummary.isNotBlank() ||
            partySummary.isNotBlank() ||
            travelPace.isNotBlank() ||
            interests.isNotEmpty() ||
            cuisinePreferences.isNotEmpty() ||
            dislikes.isNotEmpty() ||
            notes.isNotEmpty()

    fun promptSummary(): String {
        if (!hasSignals) {
            return "No structured traveler preferences have been captured yet."
        }

        return buildString {
            appendLine("Traveler profile so far:")
            appendLine("- Destination: ${destination.ifBlank { "Unknown" }}")
            appendLine("- Origin: ${origin.ifBlank { "Unknown" }}")
            appendLine("- Date window: ${dateWindow.ifBlank { "Unknown" }}")
            appendLine("- Budget: ${budgetSummary.ifBlank { "Unknown" }}")
            appendLine("- Party: ${partySummary.ifBlank { "Unknown" }}")
            appendLine("- Pace: ${travelPace.ifBlank { "Unknown" }}")
            appendLine("- Interests: ${interests.joinToString().ifBlank { "None yet" }}")
            appendLine("- Cuisine: ${cuisinePreferences.joinToString().ifBlank { "None yet" }}")
            appendLine("- Avoid: ${dislikes.joinToString().ifBlank { "None yet" }}")
            append("- Notes: ${notes.joinToString().ifBlank { "None yet" }}")
        }
    }
}
