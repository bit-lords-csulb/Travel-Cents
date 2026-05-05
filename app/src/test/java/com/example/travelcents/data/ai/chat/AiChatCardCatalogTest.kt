package com.example.travelcents.data.ai.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatCardCatalogTest {

    @Test
    fun starterCards_returnsSixUniqueCards() {
        val cards = AiChatCardCatalog.starterCards("demo-session")

        assertEquals(6, cards.size)
        assertEquals(cards.size, cards.distinctBy { it.id }.size)
    }

    @Test
    fun starterCards_isStableWithinSession() {
        val first = AiChatCardCatalog.starterCards("session-alpha").map { it.id }
        val second = AiChatCardCatalog.starterCards("session-alpha").map { it.id }

        assertEquals(first, second)
    }

    @Test
    fun starterCards_variesAcrossSessions() {
        val first = AiChatCardCatalog.starterCards("session-alpha").map { it.id }
        val second = AiChatCardCatalog.starterCards("session-beta").map { it.id }

        assertNotEquals(first, second)
        assertTrue(first.all { it.startsWith("starter_grid:") })
        assertTrue(second.all { it.startsWith("starter_grid:") })
    }
}
