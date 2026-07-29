package com.togetherly.navigation.destination

import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.quest.QuestId
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Navigation Compose's type-safe routes rely on each destination being genuinely serializable —
 * this proves it directly with `kotlinx.serialization` rather than only through a full `NavHost`,
 * so it runs everywhere (including `commonTest`) without needing a Compose UI test environment.
 * A round trip that changes the decoded value, or that fails to compile/run at all, is exactly
 * what would break process-death save/restore of the real back stack.
 */
class DestinationSerializationTest {

    @Test
    fun rootDestinationsSurviveRoundTrip() {
        assertRoundTrip(RootDestination.Bootstrap)
        assertRoundTrip(RootDestination.Onboarding)
        assertRoundTrip(RootDestination.Main)
        assertRoundTrip(RootDestination.QuestDetail(QuestId("quest-1")))
        assertRoundTrip(RootDestination.QuestMode(CompletionId("completion-1")))
    }

    @Test
    fun mainDestinationsSurviveRoundTrip() {
        assertRoundTrip(MainDestination.Today)
        assertRoundTrip(MainDestination.Explore)
        assertRoundTrip(MainDestination.Journey)
        assertRoundTrip(MainDestination.Family)
    }

    private inline fun <reified T> assertRoundTrip(value: T) {
        val encoded = Json.encodeToString(value)
        val decoded = Json.decodeFromString<T>(encoded)
        assertEquals(value, decoded)
    }
}
