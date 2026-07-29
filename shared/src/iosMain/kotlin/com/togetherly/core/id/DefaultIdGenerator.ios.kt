package com.togetherly.core.id

import platform.Foundation.NSUUID

internal actual class DefaultIdGenerator actual constructor() : IdGenerator {
    actual override fun generate(): String = NSUUID().UUIDString
}
