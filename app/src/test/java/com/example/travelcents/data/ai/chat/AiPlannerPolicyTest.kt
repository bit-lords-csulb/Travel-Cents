package com.example.travelcents.data.ai.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPlannerPolicyTest {

    @Test
    fun validatePlannerTurn_rejectsDuplicateTopicPath() {
        val context = PlannerContext(
            askedQuestionRecords = listOf(
                AskedQuestionRecord(
                    topicPath = "food_preferences",
                    questionId = "food_preferences_general",
                    answered = true,
                    answerIds = listOf("italian")
                )
            )
        )

        val validation = turn(topicPath = "food_preferences").validatePlannerTurn(context)

        assertFalse(validation.accepted)
        assertTrue(validation.rejectionReason.contains("already asked"))
    }

    @Test
    fun validatePlannerTurn_allowsAnsweredChildRefinement() {
        val context = PlannerContext(
            askedQuestionRecords = listOf(
                AskedQuestionRecord(
                    topicPath = "food_preferences",
                    questionId = "food_preferences_general",
                    answered = true,
                    answerIds = listOf("italian")
                )
            )
        )

        val validation = turn(
            topicPath = "food_preferences.italian_style",
            questionId = "italian_style_refinement",
            parentTopicPath = "food_preferences",
            parentAnswerId = "italian"
        ).validatePlannerTurn(context)

        assertTrue(validation.rejectionReason, validation.accepted)
        assertEquals("food_preferences.italian_style", validation.cardGroup?.topicPath)
        assertEquals("italian_style_refinement", validation.cardGroup?.questionId)
    }

    @Test
    fun validatePlannerTurn_rejectsInvalidOptionCount() {
        val validation = turn(
            topicPath = "budget",
            questionId = "budget_level",
            optionCount = 5
        ).validatePlannerTurn(PlannerContext())

        assertFalse(validation.accepted)
        assertTrue(validation.rejectionReason.contains("4 or 6"))
    }

    @Test
    fun validatePlannerTurn_forcesVisualWhenQuestionBudgetIsExhausted() {
        val context = PlannerContext(
            preDestinationQuestionCount = 4
        )

        val validation = turn(topicPath = "budget").validatePlannerTurn(context)

        assertFalse(validation.accepted)
        assertTrue(validation.forceVisualAction)
    }

    @Test
    fun validatePlannerTurn_forcesVisualWhenReadinessGateIsMet() {
        val context = PlannerContext(
            currentProfile = AiTripIntakeProfile(
                tripType = AiTripType.ROMANTIC,
                destinationStyle = listOf("beach"),
                budgetLevel = AiBudgetLevel.COMFORT
            ),
            visualRecommendationsDue = true
        )

        val validation = turn(topicPath = "pace").validatePlannerTurn(context)

        assertFalse(validation.accepted)
        assertTrue(validation.forceVisualAction)
        assertTrue(context.shouldForceVisualAction())
    }

    @Test
    fun repairPromptIncludesPlannerRejectionControls() {
        val prompt = PlannerContext(
            askedQuestionRecords = listOf(AskedQuestionRecord(topicPath = "budget", questionId = "budget_level"))
        ).repairPrompt("duplicate topic")

        assertTrue(prompt.contains("duplicate topic"))
        assertTrue(prompt.contains("Allowed topic roots"))
        assertTrue(prompt.contains("budget"))
        assertTrue(prompt.contains("exactly 4 or exactly 6"))
    }

    @Test
    fun fallbackQuestionGroupUsesTopicSpecificPlannerCardNotDeadEnd() {
        val group = AiChatCardCatalog.fallbackQuestionGroup("budget")

        assertNotNull(group)
        assertNotEquals(AiChatCardCatalog.DEAD_END_GROUP_ID, group?.id)
        assertEquals("budget", group?.topicPath)
        assertEquals(PlannerQuestionSource.APP_FALLBACK, group?.source)
        assertEquals(4, group?.options?.size)
    }

    @Test
    fun nextBestAllowedTopicPathReturnsNullWhenVisualIsDue() {
        val topicPath = PlannerContext(
            currentProfile = AiTripIntakeProfile(
                tripType = AiTripType.FRIENDS,
                interests = listOf("food", "nightlife"),
                budgetLevel = AiBudgetLevel.BUDGET
            ),
            visualRecommendationsDue = true
        ).nextBestAllowedTopicPath()

        assertNull(topicPath)
    }

    private fun turn(
        topicPath: String,
        questionId: String = topicPath.replace('.', '_'),
        parentTopicPath: String = "",
        parentAnswerId: String = "",
        optionCount: Int = 4
    ): AiTripIntakeTurnResult {
        return AiTripIntakeTurnResult(
            assistantMessageText = "Got it.",
            nextAction = AiTripIntakeNextAction.ASK_MORE,
            topicPath = topicPath,
            questionId = questionId,
            parentTopicPath = parentTopicPath,
            parentAnswerId = parentAnswerId,
            questionTitle = "Pick one",
            options = (1..optionCount).map { index ->
                AiTripIntakeAnswerOption(
                    id = "option_$index",
                    label = "Option $index",
                    message = "Option $index."
                )
            }
        )
    }
}
