package com.togetherly.core.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LocalizedDateTimeFormattersTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun englishLocaleUsesA12HourClockAndAmPm() {
        Locale.setDefault(Locale.US)

        val formatted = LocalTime(15, 5).localizedTimeDisplay()

        assertTrue(formatted.contains("3:05"))
        assertTrue(formatted.contains("PM"))
    }

    @Test
    fun germanLocaleUsesA24HourClock() {
        Locale.setDefault(Locale.GERMANY)

        val formatted = LocalTime(15, 5).localizedTimeDisplay()

        assertTrue(formatted.contains("15:05"))
        assertFalse(formatted.contains("AM"))
        assertFalse(formatted.contains("PM"))
    }

    @Test
    fun dateFormattingVariesAcrossLocales() {
        val sample = LocalDateTime(2026, 7, 4, 15, 5)

        Locale.setDefault(Locale.US)
        val us = sample.localizedDateDisplay()

        Locale.setDefault(Locale.GERMANY)
        val de = sample.localizedDateDisplay()

        assertNotEquals(us, de)
    }
}
