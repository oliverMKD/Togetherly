package com.togetherly.domain.recommendation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StableHashTest {

    @Test
    fun knownInputProducesAFixedValue() {
        // A plain FNV-1a 64-bit hash of this exact string — computable independently of this
        // project on any platform, which is the whole point (see stableHash's own KDoc).
        assertEquals(-5846310652949924999L, stableHash("family-1|2026-06-15|0|quest-1"))
    }

    @Test
    fun sameInputAlwaysProducesTheSameHash() {
        assertEquals(stableHash("family-1|2026-06-15|0|quest-1"), stableHash("family-1|2026-06-15|0|quest-1"))
    }

    @Test
    fun differentInputsProduceDifferentHashes() {
        assertNotEquals(stableHash("family-1|2026-06-15|0|quest-1"), stableHash("family-1|2026-06-15|1|quest-1"))
    }
}
