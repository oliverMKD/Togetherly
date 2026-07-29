package com.togetherly.core.net

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalUrlLauncherTest {

    @Test
    fun validHttpsUrlIsAccepted() {
        assertTrue(isValidExternalUrl("https://example.com/togetherly/privacy"))
    }

    @Test
    fun validMailtoUrlIsAccepted() {
        assertTrue(isValidExternalUrl("mailto:support@example.com"))
    }

    @Test
    fun blankUrlIsRejected() {
        assertFalse(isValidExternalUrl(""))
        assertFalse(isValidExternalUrl("   "))
    }

    @Test
    fun unsupportedSchemeIsRejected() {
        assertFalse(isValidExternalUrl("http://example.com"))
        assertFalse(isValidExternalUrl("javascript:alert(1)"))
        assertFalse(isValidExternalUrl("ftp://example.com"))
    }

    @Test
    fun httpsUrlWithNothingAfterSchemeIsRejected() {
        assertFalse(isValidExternalUrl("https://"))
    }

    @Test
    fun mailtoUrlWithoutAddressIsRejected() {
        assertFalse(isValidExternalUrl("mailto:"))
        assertFalse(isValidExternalUrl("mailto:not-an-address"))
    }
}
