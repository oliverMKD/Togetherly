package com.togetherly.core.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [AppLocaleController.init]/[AppLocaleController.select] both call the real [applyPlatformLocale]
 * as a side effect (there's no fake seam for it — it's a thin platform primitive, not a
 * collaborator worth abstracting behind an interface just for this test). Every test that leaves a
 * non-null override active restores it to "follow system" (`select(null)`) in a `finally` block so
 * it can't leak into whichever test the JVM test runner happens to execute next (this matters in
 * particular for `Locale.getDefault()`, which date/time formatting elsewhere in the app also reads).
 */
class AppLocaleControllerTest {

    @Test
    fun initialLanguageMirrorsWhateverTheStoreAlreadyHad() {
        val store = FakeAppLocaleStore(initial = "de")
        val controller = AppLocaleController(store)
        try {
            assertEquals("de", controller.language.value)
        } finally {
            controller.select(null)
        }
    }

    @Test
    fun noPersistedChoiceMeansFollowSystem() {
        val store = FakeAppLocaleStore(initial = null)
        val controller = AppLocaleController(store)

        assertNull(controller.language.value)
    }

    @Test
    fun selectingALanguagePersistsItThroughTheStore() {
        val store = FakeAppLocaleStore(initial = null)
        val controller = AppLocaleController(store)
        try {
            controller.select("mk")

            assertEquals("mk", controller.language.value)
            assertEquals("mk", store.getSelectedLanguage())
        } finally {
            controller.select(null)
        }
    }

    @Test
    fun selectingNullReturnsToFollowingSystemAndClearsTheStore() {
        val store = FakeAppLocaleStore(initial = "fr")
        val controller = AppLocaleController(store)

        controller.select(null)

        assertNull(controller.language.value)
        assertNull(store.getSelectedLanguage())
    }
}
