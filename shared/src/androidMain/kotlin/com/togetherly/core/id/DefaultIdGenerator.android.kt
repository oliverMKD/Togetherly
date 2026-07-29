package com.togetherly.core.id

import java.util.UUID

internal actual class DefaultIdGenerator actual constructor() : IdGenerator {
    actual override fun generate(): String = UUID.randomUUID().toString()
}
