package com.togetherly.domain.recommendation

/** The FNV-1a 64-bit offset basis and prime — see [stableHash]'s own KDoc. */
private const val FNV_OFFSET_BASIS = -3750763034362895579L // 0xcbf29ce484222325
private const val FNV_PRIME = 1099511628211L

/**
 * A plain [FNV-1a](http://www.isthe.com/chongo/tech/comp/fnv/) 64-bit hash over [input]'s UTF-16
 * code units, implemented from scratch rather than delegating to [String.hashCode] — Kotlin only
 * guarantees [String.hashCode]'s exact algorithm on the JVM; Kotlin/Native's is not contractually
 * required to produce the same value for the same string. [DeterministicQuestRecommendationPolicy]'s
 * tie-break key must produce the *identical* result on Android and iOS for the same inputs, so it
 * needs an algorithm this file owns completely rather than one delegated to the platform.
 */
internal fun stableHash(input: String): Long {
    var hash = FNV_OFFSET_BASIS
    for (char in input) {
        hash = hash xor char.code.toLong()
        hash *= FNV_PRIME
    }
    return hash
}
