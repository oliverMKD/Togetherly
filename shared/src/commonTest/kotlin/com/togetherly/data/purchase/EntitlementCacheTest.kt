package com.togetherly.data.purchase

import com.togetherly.data.local.database.DatabaseMetadataEntity
import com.togetherly.data.local.database.FakeDatabaseMetadataDao
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.AccessSource
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

/**
 * The one place [EntitlementCache]'s own serialize/deserialize round-trip and its lifetime-vs-cached
 * distinction are exercised directly — [RevenueCatEntitlementRepositoryTest] only proves this class
 * is *called* correctly, not what it does with malformed or previously-written data on its own.
 */
class EntitlementCacheTest {

    @Test
    fun loadReturnsNullWhenNothingWasEverCached() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())

        assertNull(cache.load())
    }

    @Test
    fun freeAccessRoundTrips() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())
        val snapshot = AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)

        cache.save(snapshot)
        val loaded = cache.load()

        assertEquals(snapshot, loaded)
    }

    @Test
    fun subscriptionAccessRoundTripsAsCachedSourceNotSubscription() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())
        val expiresAt = NOW + 30.days
        val snapshot = AccessSnapshot(
            FamilyAccess.subscription(expiresAt = expiresAt, willRenew = true),
            setOf(EntitlementId("family_plus")),
            NOW,
        )

        cache.save(snapshot)
        val loaded = cache.load()

        // A reloaded snapshot is, by definition, no longer a live-verified subscription — it
        // becomes AccessSource.CACHED (subject to QuestAccessPolicy's offline grace period),
        // never re-presented as a still-live AccessSource.SUBSCRIPTION.
        assertEquals(AccessSource.CACHED, loaded?.familyAccess?.source)
        assertEquals(true, loaded?.familyAccess?.isPlus)
        assertEquals(expiresAt, loaded?.familyAccess?.expiresAt)
    }

    @Test
    fun lifetimeAccessRoundTripsAsLifetimeNotCached() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())
        val snapshot = AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId("family_plus")), NOW)

        cache.save(snapshot)
        val loaded = cache.load()

        // Lifetime access must never lapse just because it's a reload — it stays exempt from the
        // offline grace period a merely-cached subscription is subject to.
        assertEquals(AccessSource.LIFETIME, loaded?.familyAccess?.source)
        assertNull(loaded?.familyAccess?.expiresAt)
    }

    @Test
    fun malformedStoredValueIsIgnoredRatherThanCrashing() = runTest {
        val dao = FakeDatabaseMetadataDao()
        dao.set(DatabaseMetadataEntity(key = "entitlement_access_snapshot", value = "not valid json"))
        val cache = EntitlementCache(dao)

        assertNull(cache.load())
    }

    @Test
    fun savingOverwritesThePreviousSnapshot() = runTest {
        val cache = EntitlementCache(FakeDatabaseMetadataDao())
        cache.save(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))

        val premium = AccessSnapshot(FamilyAccess.lifetime(), setOf(EntitlementId("family_plus")), NOW)
        cache.save(premium)
        val loaded = cache.load()

        assertEquals(true, loaded?.familyAccess?.isPlus)
    }
}
