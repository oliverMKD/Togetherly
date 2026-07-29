package com.togetherly.core.id

class SequentialIdGenerator(
    private val prefix: String = "id",
) : IdGenerator {
    private var counter = 0

    override fun generate(): String = "$prefix-${counter++}"
}
