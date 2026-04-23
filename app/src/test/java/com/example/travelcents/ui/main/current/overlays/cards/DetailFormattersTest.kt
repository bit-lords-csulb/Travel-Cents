package com.example.travelcents.ui.main.current.overlays.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailFormattersTest {

    @Test
    fun formatPrice_roundWholeDollars() {
        assertEquals("$1,234", formatPrice(1234.0))
    }

    @Test
    fun formatPrice_keepCentsWhenNeeded() {
        assertEquals("$1,234.50", formatPrice(1234.5))
    }

    @Test
    fun formatPrice_parseCurrencyText() {
        assertEquals("$1,234.50", formatPrice("$1,234.50"))
    }

    @Test
    fun formatPrice_returnNullForInvalidText() {
        assertNull(formatPrice("not-a-price"))
        assertNull(formatPrice(null))
    }
}
