package com.togetherly.domain.purchase

import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

private val EXPIRES_AT = Instant.parse("2026-08-01T00:00:00Z")

class FamilyAccessTest {

    @Test
    fun validFreeAccessIsAccepted() {
        val access = FamilyAccess.free()

        assertEquals(false, access.isPlus)
        assertEquals(AccessSource.FREE, access.source)
        assertEquals(null, access.expiresAt)
        assertEquals(null, access.willRenew)
    }

    @Test
    fun validRenewingSubscriptionIsAccepted() {
        val access = FamilyAccess.subscription(expiresAt = EXPIRES_AT, willRenew = true)

        assertEquals(true, access.isPlus)
        assertEquals(EXPIRES_AT, access.expiresAt)
        assertEquals(true, access.willRenew)
    }

    @Test
    fun validNonRenewingSubscriptionIsAccepted() {
        val access = FamilyAccess.subscription(expiresAt = EXPIRES_AT, willRenew = false)

        assertEquals(true, access.isPlus)
        assertEquals(false, access.willRenew)
    }

    @Test
    fun validLifetimeAccessIsAccepted() {
        val access = FamilyAccess.lifetime()

        assertEquals(true, access.isPlus)
        assertEquals(null, access.expiresAt)
        assertEquals(null, access.willRenew)
    }

    @Test
    fun validPromotionalAccessIsAccepted() {
        val indefinite = FamilyAccess.promotional(expiresAt = null)
        val timeLimited = FamilyAccess.promotional(expiresAt = EXPIRES_AT)

        assertEquals(true, indefinite.isPlus)
        assertEquals(null, indefinite.expiresAt)
        assertEquals(true, timeLimited.isPlus)
        assertEquals(EXPIRES_AT, timeLimited.expiresAt)
    }

    @Test
    fun validCachedAccessIsAccepted() {
        val cachedPlus = FamilyAccess.cached(isPlus = true, expiresAt = EXPIRES_AT)
        val cachedFree = FamilyAccess.cached(isPlus = false, expiresAt = null)

        assertEquals(AccessSource.CACHED, cachedPlus.source)
        assertEquals(null, cachedPlus.willRenew)
        assertEquals(false, cachedFree.isPlus)
    }

    @Test
    fun contradictoryCombinationsCannotBeCreated() {
        val freeWithExpiration = assertFailsWith<DomainValidationException> {
            FamilyAccess(isPlus = false, source = AccessSource.FREE, expiresAt = EXPIRES_AT, willRenew = null)
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, freeWithExpiration.reason)

        val freeThatIsPlus = assertFailsWith<DomainValidationException> {
            FamilyAccess(isPlus = true, source = AccessSource.FREE, expiresAt = null, willRenew = null)
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, freeThatIsPlus.reason)

        val subscriptionWithoutExpiration = assertFailsWith<DomainValidationException> {
            FamilyAccess(isPlus = true, source = AccessSource.SUBSCRIPTION, expiresAt = null, willRenew = true)
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, subscriptionWithoutExpiration.reason)

        val lifetimeWithExpiration = assertFailsWith<DomainValidationException> {
            FamilyAccess(isPlus = true, source = AccessSource.LIFETIME, expiresAt = EXPIRES_AT, willRenew = null)
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, lifetimeWithExpiration.reason)

        val lifetimeThatRenews = assertFailsWith<DomainValidationException> {
            FamilyAccess(isPlus = true, source = AccessSource.LIFETIME, expiresAt = null, willRenew = false)
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, lifetimeThatRenews.reason)

        val promotionalThatRenews = assertFailsWith<DomainValidationException> {
            FamilyAccess(isPlus = true, source = AccessSource.PROMOTIONAL, expiresAt = null, willRenew = true)
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, promotionalThatRenews.reason)

        val cachedThatRenews = assertFailsWith<DomainValidationException> {
            FamilyAccess(isPlus = true, source = AccessSource.CACHED, expiresAt = null, willRenew = true)
        }
        assertEquals(DomainValidationReason.CONTRADICTORY_STATE, cachedThatRenews.reason)
    }
}
