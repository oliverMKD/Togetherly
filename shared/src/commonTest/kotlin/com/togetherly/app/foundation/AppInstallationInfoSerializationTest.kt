package com.togetherly.app.foundation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppInstallationInfoSerializationTest {

    @Test
    fun jsonRoundTripPreservesEquality() {
        val original = AppInstallationInfo(
            installationId = "test-installation-id",
            createdAtEpochMilliseconds = 1_700_000_000_000L,
        )

        val json = Json.encodeToString(AppInstallationInfo.serializer(), original)
        val decoded = Json.decodeFromString(AppInstallationInfo.serializer(), json)

        assertEquals(original, decoded)
    }
}
