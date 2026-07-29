package com.togetherly.domain.journey

/**
 * A from-scratch FNV-1a hash over UTF-16 code units — deliberately not [String.hashCode], whose
 * algorithm is only specified for the JVM. Kotlin/Native and Kotlin/JS are free to implement it
 * differently, which would silently put the same family's same completion at a different star
 * position on Android vs. iOS. Every caller in this package (see [JourneyStarPolicy]) must derive
 * star placement only through this function.
 */
private const val FNV_OFFSET_BASIS: UInt = 2166136261u
private const val FNV_PRIME: UInt = 16777619u

internal fun stableHash(value: String): UInt {
    var hash = FNV_OFFSET_BASIS
    for (char in value) {
        hash = hash xor char.code.toUInt()
        hash *= FNV_PRIME
    }
    return hash
}

/** Maps a [stableHash] result onto a fraction of `0f..1f`, inclusive at both ends. */
internal fun UInt.toUnitFraction(): Float = (this.toDouble() / UInt.MAX_VALUE.toDouble()).toFloat()
