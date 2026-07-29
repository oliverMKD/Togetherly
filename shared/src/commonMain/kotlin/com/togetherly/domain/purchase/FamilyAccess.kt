package com.togetherly.domain.purchase

import com.togetherly.domain.validation.requireConsistentState
import kotlin.time.Instant

/**
 * The constructor is internal and every valid shape outside this module is reachable only
 * through the companion factories, so consumers can't build a contradictory combination
 * (e.g. LIFETIME with an expiration) — not even via [copy], whose visibility Kotlin ties to
 * the primary constructor's. The constructor stays internal rather than private so this
 * module's own tests can exercise the invariant checks directly.
 */
data class FamilyAccess internal constructor(
    val isPlus: Boolean,
    val source: AccessSource,
    val expiresAt: Instant?,
    val willRenew: Boolean?,
) {
    init {
        when (source) {
            AccessSource.FREE -> {
                requireConsistentState(!isPlus)
                requireConsistentState(expiresAt == null)
                requireConsistentState(willRenew == null)
            }
            AccessSource.SUBSCRIPTION -> {
                requireConsistentState(isPlus)
                requireConsistentState(expiresAt != null)
                requireConsistentState(willRenew != null)
            }
            AccessSource.LIFETIME -> {
                requireConsistentState(isPlus)
                requireConsistentState(expiresAt == null)
                requireConsistentState(willRenew == null)
            }
            AccessSource.PROMOTIONAL -> {
                requireConsistentState(isPlus)
                requireConsistentState(willRenew == null)
            }
            AccessSource.CACHED -> {
                requireConsistentState(willRenew == null)
            }
        }
    }

    companion object {
        fun free(): FamilyAccess = FamilyAccess(
            isPlus = false,
            source = AccessSource.FREE,
            expiresAt = null,
            willRenew = null,
        )

        fun subscription(expiresAt: Instant, willRenew: Boolean): FamilyAccess = FamilyAccess(
            isPlus = true,
            source = AccessSource.SUBSCRIPTION,
            expiresAt = expiresAt,
            willRenew = willRenew,
        )

        fun lifetime(): FamilyAccess = FamilyAccess(
            isPlus = true,
            source = AccessSource.LIFETIME,
            expiresAt = null,
            willRenew = null,
        )

        fun promotional(expiresAt: Instant?): FamilyAccess = FamilyAccess(
            isPlus = true,
            source = AccessSource.PROMOTIONAL,
            expiresAt = expiresAt,
            willRenew = null,
        )

        /**
         * [isPlus] and [expiresAt] reflect the last-known verified state. [willRenew] is never
         * knowable offline, so cached access never carries a renewal state.
         */
        fun cached(isPlus: Boolean, expiresAt: Instant?): FamilyAccess = FamilyAccess(
            isPlus = isPlus,
            source = AccessSource.CACHED,
            expiresAt = expiresAt,
            willRenew = null,
        )
    }
}
