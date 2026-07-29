package com.togetherly.core.id

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

private class SequentialTestIdGenerator : IdGenerator {
    private var counter = 0

    override fun generate(): String = "test-id-${counter++}"
}

class IdGeneratorTest {

    @Test
    fun generatedValuesAreNotBlank() {
        val generator: IdGenerator = SequentialTestIdGenerator()

        assertFalse(generator.generate().isBlank())
    }

    @Test
    fun sequentialTestValuesAreDeterministic() {
        val generator = SequentialTestIdGenerator()

        assertEquals("test-id-0", generator.generate())
        assertEquals("test-id-1", generator.generate())
        assertEquals("test-id-2", generator.generate())
    }

    @Test
    fun multipleProductionIdsAreNotIdentical() {
        val generator: IdGenerator = DefaultIdGenerator()

        val first = generator.generate()
        val second = generator.generate()

        assertFalse(first.isBlank())
        assertFalse(second.isBlank())
        assertNotEquals(first, second)
    }
}
