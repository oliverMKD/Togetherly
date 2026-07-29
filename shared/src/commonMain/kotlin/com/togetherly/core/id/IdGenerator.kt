package com.togetherly.core.id

interface IdGenerator {
    fun generate(): String
}

internal expect class DefaultIdGenerator() : IdGenerator {
    override fun generate(): String
}
