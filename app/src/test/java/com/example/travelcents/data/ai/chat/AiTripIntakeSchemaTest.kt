package com.example.travelcents.data.ai.chat

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTripIntakeSchemaTest {

    @Test
    fun toCardGroup_addsOtherOptionAndCapsVisibleOptionsAtSix() {
        val question = AiTripIntakeFollowUpQuestion(
            id = "food preferences",
            topicPath = "food_preferences",
            title = "What food sounds right?",
            allowMultiple = true,
            allowOther = true,
            otherPromptHint = "Type the cuisines or dishes you want.",
            options = listOf(
                option("street_food", "Street food"),
                option("seafood", "Seafood"),
                option("fine_dining", "Fine dining"),
                option("cafes", "Coffee cafes"),
                option("desserts", "Dessert spots"),
                option("markets", "Night markets")
            )
        )

        val group = question.toCardGroup()

        assertNotNull(group)
        assertEquals("food_preferences", group?.id)
        assertEquals("food_preferences", group?.topicPath)
        assertEquals("food_preferences", group?.questionId)
        assertEquals(PlannerQuestionSource.LLM, group?.source)
        assertEquals(6, group?.options?.size)
        assertTrue(group?.allowOther == true)
        assertTrue(group?.options?.lastOrNull()?.requiresText == true)
        assertEquals("Other", group?.options?.lastOrNull()?.label)
    }

    @Test
    fun toCardGroup_keepsLabelsWithinFourWords() {
        val question = AiTripIntakeFollowUpQuestion(
            id = "party_shape",
            topicPath = "traveler_context",
            title = "Who is going?",
            options = listOf(
                option("family_with_kids", "Family with kids"),
                option("group_of_friends", "Group of friends"),
                option("couple", "Couple trip"),
                option("solo", "Solo trip")
            )
        )

        val group = question.toCardGroup()

        assertNotNull(group)
        assertEquals(
            listOf("Family with kids", "Group of friends", "Couple trip", "Solo trip"),
            group?.options?.map { option -> option.label }
        )
    }

    @Test
    fun toCardGroup_returnsNullWhenNotEnoughUsableOptionsRemain() {
        val question = AiTripIntakeFollowUpQuestion(
            id = "pace",
            topicPath = "pace",
            title = "Pick a pace",
            allowOther = false,
            options = listOf(
                option("blank", " "),
                option("also_blank", "")
            )
        )

        assertNull(question.toCardGroup())
    }

    @Test
    fun toCardGroup_marksModelSuppliedOtherAsRequiresText() {
        val question = AiTripIntakeFollowUpQuestion(
            id = "hotel_area",
            topicPath = "hotel_preferences.area",
            title = "Which area?",
            allowOther = true,
            options = listOf(
                option("beachfront", "Beachfront"),
                option("downtown", "Downtown"),
                option("quiet", "Quiet area"),
                option("other", "Other")
            )
        )

        val group = question.toCardGroup()

        assertNotNull(group)
        assertTrue(group?.options?.any { option -> option.requiresText } == true)
        assertTrue((group?.options?.count { option -> option.label == "Other" } ?: 0) <= 1)
    }

    @Test
    fun toDestinationRecommendationRow_filtersDuplicatesAndCapsAtThree() {
        val row = AiTripIntakeTurnResult(
            destinationRecommendations = listOf(
                destinationRecommendation("tokyo_main", "Tokyo"),
                destinationRecommendation("tokyo_duplicate", "Tokyo"),
                destinationRecommendation("rome_main", "Rome"),
                destinationRecommendation("bali_main", "Bali"),
                AiTripIntakeDestinationRecommendation(id = "blank_destination", destination = "", summary = "Nope", reason = "Nope")
            )
        ).toDestinationRecommendationRow()

        assertNotNull(row)
        assertEquals(3, row?.recommendations?.size)
        assertEquals(listOf("Tokyo", "Rome", "Bali"), row?.recommendations?.map { recommendation -> recommendation.destination })
        assertEquals("tokyo_food_neighborhoods", row?.recommendations?.firstOrNull()?.seedId)
    }

    @Test
    fun toPlaceRecommendationRow_filtersDuplicatesAndCapsAtThree() {
        val row = AiTripIntakeTurnResult(
            placeRecommendations = listOf(
                placeRecommendation("ari_main", "Ari", "Neighborhood"),
                placeRecommendation("ari_duplicate", "Ari", "Neighborhood"),
                placeRecommendation("old_town", "Old Town", "Historic area"),
                placeRecommendation("sukhumvit", "Sukhumvit", "Neighborhood"),
                AiTripIntakePlaceRecommendation(id = "blank_name", name = "", category = "Area", summary = "Nope", reason = "Nope")
            )
        ).toPlaceRecommendationRow()

        assertNotNull(row)
        assertEquals(3, row?.recommendations?.size)
        assertEquals(
            listOf("Ari", "Old Town", "Sukhumvit"),
            row?.recommendations?.map { recommendation -> recommendation.name }
        )
    }

    @Test
    fun assistantMessage_returnsAckOnlyEvenWithCardPayload() {
        val result = AiTripIntakeTurnResult(
            ackKey = AiTripIntakeAckKey.UNDERSTOOD,
            assistantMessageText = "Understood.",
            nextAction = AiTripIntakeNextAction.ASK_MORE,
            topicPath = "traveler_context",
            questionId = "trip_type",
            questionTitle = "What type of trip are you planning?",
            options = listOf(
                option("romantic", "Romantic"),
                option("family", "Family"),
                option("solo", "Solo"),
                option("friends", "Friends")
            )
        )

        assertEquals("Understood.", result.assistantMessage)
    }

    @Test
    fun followUpQuestion_isDerivedFromMinimalAskMorePayload() {
        val result = AiTripIntakeTurnResult(
            nextAction = AiTripIntakeNextAction.ASK_MORE,
            topicPath = "destination_style",
            questionId = "destination_type",
            questionTitle = "What kind of destination?",
            options = listOf(
                option("tropical", "Tropical"),
                option("coastal_town", "Coastal town"),
                option("beach_resort", "Beach resort"),
                option("hidden_cove", "Hidden cove")
            )
        )

        val followUpQuestion = result.followUpQuestion

        assertNotNull(followUpQuestion)
        assertEquals("destination_type", followUpQuestion?.id)
        assertEquals("destination_style", followUpQuestion?.topicPath)
        assertEquals("What kind of destination?", followUpQuestion?.title)
        assertEquals(4, followUpQuestion?.options?.size)
    }

    @Test
    fun assistantMessage_ignoresLegacyTextPromptWithoutCardPayload() {
        val result = AiTripIntakeTurnResult(
            ackKey = AiTripIntakeAckKey.PERFECT,
            assistantMessageText = "Perfect.",
            nextAction = AiTripIntakeNextAction.ASK_MORE,
            textPrompt = "Tell me more about your dates."
        )

        assertEquals("Perfect.", result.assistantMessage)
        assertNull(result.followUpQuestion)
    }

    @Test
    fun followUpQuestion_isSuppressedWhenNextActionIsNotAskMore() {
        val result = AiTripIntakeTurnResult(
            assistantMessageText = "Got it!",
            nextAction = AiTripIntakeNextAction.BUILD_TRIP,
            questionKind = AiTripIntakeQuestionKind.CARDS,
            topicPath = "budget",
            questionId = "budget",
            questionTitle = "What's your budget?",
            options = listOf(
                option("budget", "Budget"),
                option("comfort", "Comfort"),
                option("splurge", "Some splurges"),
                option("luxury", "Luxury")
            )
        )

        assertEquals("Got it!", result.assistantMessage)
        assertNull(result.followUpQuestion)
    }

    @Test
    fun toPromptJson_usesSchemaFriendlyValuesOnly() {
        val profile = AiTripIntakeProfile(
            tripType = AiTripType.ROMANTIC,
            partySummary = "Two adults",
            destinationStyle = listOf("Beach", "Warm_Weather"),
            budgetLevel = AiBudgetLevel.LUXURY,
            pace = AiTripPacePreference.RELAXED,
            interests = listOf("Beach"),
            mustHaves = listOf("Ocean view"),
            avoid = listOf("Crowds"),
            notes = listOf("Shoulder season"),
            durationDays = 5,
            budgetTotal = 2500.0,
            cuisinePreferences = listOf("Seafood"),
            confidence = mapOf("pace" to 0.8)
        )

        val root = JsonParser.parseString(profile.toPromptJson()).asJsonObject

        assertEquals("romantic", root.get("trip_type").asString)
        assertEquals("luxury", root.get("budget_level").asString)
        assertEquals("relaxed", root.get("pace").asString)
        assertEquals("beach", root.getAsJsonArray("destination_style")[0].asString)
        assertEquals("warm_weather", root.getAsJsonArray("destination_style")[1].asString)
        assertFalse(root.has("duration_days"))
        assertFalse(root.has("budget_total"))
        assertFalse(root.has("cuisine_preferences"))
        assertFalse(root.has("confidence"))
    }

    private fun option(id: String, label: String): AiTripIntakeAnswerOption {
        return AiTripIntakeAnswerOption(
            id = id,
            label = label,
            message = "$label sounds right."
        )
    }

    private fun destinationRecommendation(
        id: String,
        destination: String
    ): AiTripIntakeDestinationRecommendation {
        return AiTripIntakeDestinationRecommendation(
            id = id,
            destination = destination,
            summary = "$destination fits the trip.",
            reason = "Good fit"
        )
    }

    private fun placeRecommendation(
        id: String,
        name: String,
        category: String
    ): AiTripIntakePlaceRecommendation {
        return AiTripIntakePlaceRecommendation(
            id = id,
            name = name,
            category = category,
            area = "Central",
            summary = "$name suits the trip.",
            reason = "Strong fit"
        )
    }
}
