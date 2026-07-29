package com.togetherly.data.purchase

import com.togetherly.data.local.database.DatabaseMetadataDao
import com.togetherly.data.local.database.DatabaseMetadataEntity
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.AccessSource
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.purchase.FamilyAccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

private const val METADATA_KEY = "entitlement_access_snapshot"

/**
 * Only the minimum needed to present a stable offline state — never a receipt, a purchase token,
 * or anything RevenueCat wouldn't already show a family in their own store account. Reuses
 * [DatabaseMetadataDao] (Step 6.1's small key-value table) rather than adding a new Room table,
 * per this feature's own task spec.
 */
@Serializable
internal data class CachedAccessSnapshot(
    val isPlus: Boolean,
    val source: String,
    val expiresAtEpochMillis: Long?,
    val willRenew: Boolean?,
    val verifiedAtEpochMillis: Long,
)

internal class EntitlementCache(
    private val metadataDao: DatabaseMetadataDao,
) {
    suspend fun load(): AccessSnapshot? {
        val raw = metadataDao.getValue(METADATA_KEY) ?: return null
        return runCatching { Json.decodeFromString<CachedAccessSnapshot>(raw).toAccessSnapshot() }.getOrNull()
    }

    suspend fun save(snapshot: AccessSnapshot) {
        val cached = snapshot.toCached()
        metadataDao.set(DatabaseMetadataEntity(key = METADATA_KEY, value = Json.encodeToString(CachedAccessSnapshot.serializer(), cached)))
    }

    /**
     * Deletes only this cache's own `database_metadata` row — never the whole table, which also
     * holds unrelated schema/migration bookkeeping (see [com.togetherly.data.local.database.TogetherlyDatabase]'s
     * own KDoc). Used by a full local-data wipe ([com.togetherly.domain.localdata.usecase.DeleteAllLocalData]);
     * never a substitute for [com.togetherly.domain.purchase.repository.EntitlementRepository.clearCache]'s
     * own KDoc on what this is and is not (never a subscription cancellation, never a RevenueCat
     * server-side change).
     */
    suspend fun clear() {
        metadataDao.delete(METADATA_KEY)
    }
}

private fun AccessSnapshot.toCached(): CachedAccessSnapshot = CachedAccessSnapshot(
    isPlus = familyAccess.isPlus,
    source = familyAccess.source.name,
    expiresAtEpochMillis = familyAccess.expiresAt?.toEpochMilliseconds(),
    willRenew = familyAccess.willRenew,
    verifiedAtEpochMillis = verifiedAt.toEpochMilliseconds(),
)

/**
 * Lifetime access is preserved as [AccessSource.LIFETIME] (never downgraded to
 * [AccessSource.CACHED]) so it stays exempt from [com.togetherly.domain.purchase.QuestAccessPolicy]'s
 * offline grace-period check — a lifetime purchase never expires, cached or not. Any other cached
 * premium access becomes [AccessSource.CACHED], which *is* subject to that policy's grace period,
 * so a family's premium access left unrefreshed for too long safely lapses rather than staying
 * premium forever on stale local data alone.
 */
private fun CachedAccessSnapshot.toAccessSnapshot(): AccessSnapshot {
    val access = when {
        !isPlus -> FamilyAccess.free()
        source == AccessSource.LIFETIME.name -> FamilyAccess.lifetime()
        else -> FamilyAccess.cached(
            isPlus = true,
            expiresAt = expiresAtEpochMillis?.let(Instant::fromEpochMilliseconds),
        )
    }
    return AccessSnapshot(
        familyAccess = access,
        activeEntitlements = if (isPlus) setOf(EntitlementId(FAMILY_PLUS_ENTITLEMENT_ID)) else emptySet(),
        verifiedAt = Instant.fromEpochMilliseconds(verifiedAtEpochMillis),
    )
}
