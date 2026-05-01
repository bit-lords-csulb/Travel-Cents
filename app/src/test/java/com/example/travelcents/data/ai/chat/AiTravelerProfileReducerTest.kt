package com.example.travelcents.data.ai.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class AiTravelerProfileReducerTest {

    @Test
    fun merge_recognizesSeededDestinationSelections() {
        val cancunProfile = AiTravelerProfileReducer.merge(
            profile = AiTravelerProfile(),
            userInput = "Destination choice: Cancun sounds like the best fit for this trip."
        )
        val bangkokProfile = AiTravelerProfileReducer.merge(
            profile = AiTravelerProfile(),
            userInput = "Let's go with Bangkok."
        )

        assertEquals("Cancun", cancunProfile.destination)
        assertEquals("Bangkok", bangkokProfile.destination)
    }
}
