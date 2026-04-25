package com.example.travelcents.data.ai.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTripIntakeSchemaTest {

    @Test
    fun toCardGroup_addsOtherOptionAndCapsVisibleOptionsAtSix() {
        val question = AiTripIntakeFollowUpQuestion(
            id = "food preferences",
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
        assertEquals(6, group?.options?.size)
        assertTrue(group?.allowOther == true)
        assertTrue(group?.options?.lastOrNull()?.requiresText == true)
        assertEquals("Other", group?.options?.lastOrNull()?.label)
    }

    @Test
    fun toCardGroup_shortensLabelsToTwoWords() {
        val question = AiTripIntakeFollowUpQuestion(
            id = "party_shape",
            title = "Who is going?",
            options = listOf(
                option("family_with_kids", "Family with kids"),
                option("group_of_friends", "Group of friends")
            )
        )

        val group = question.toCardGroup()

        assertNotNull(group)
        assertEquals(listOf("Family with", "Group of"), group?.options?.map { option -> option.label })
    }

    @Test
    fun toCardGroup_returnsNullWhenNotEnoughUsableOptionsRemain() {
        val question = AiTripIntakeFollowUpQuestion(
            id = "pace",
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
            title = "Which area?",
            allowOther = true,
            options = listOf(
                option("beachfront", "Beachfront"),
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
