package com.togetherly.data.telemetry

import com.togetherly.data.local.database.DatabaseMetadataDao
import com.togetherly.data.local.database.DatabaseMetadataEntity
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val METADATA_KEY = "telemetry_consent"

/**
 * Only the two decisions themselves — never a device identifier, never anything that would let
 * this row double as a hidden analytics identity. Reuses [DatabaseMetadataDao] (Step 6.1's small
 * key-value table) rather than adding a new Room table or migration, the same choice
 * [com.togetherly.data.purchase.EntitlementCache] already made for the same reason.
 */
@Serializable
internal data class CachedTelemetryConsent(
    val analytics: String,
    val diagnostics: String,
)

internal class TelemetryConsentCache(
    private val metadataDao: DatabaseMetadataDao,
) {
    /** `null` means nothing has ever been persisted — a caller should treat this as [TelemetryConsent.default], never as an error. */
    suspend fun load(): TelemetryConsent? {
        val raw = runCatching { metadataDao.getValue(METADATA_KEY) }.getOrNull() ?: return null
        return runCatching { Json.decodeFromString<CachedTelemetryConsent>(raw).toTelemetryConsent() }.getOrNull()
    }

    suspend fun save(consent: TelemetryConsent) {
        runCatching {
            val cached = consent.toCached()
            metadataDao.set(DatabaseMetadataEntity(key = METADATA_KEY, value = Json.encodeToString(CachedTelemetryConsent.serializer(), cached)))
        }
    }

    /**
     * Deletes only this cache's own `database_metadata` row — never the whole table, which also
     * holds unrelated schema/migration bookkeeping and the entitlement cache. Used by a full
     * local-data wipe ([com.togetherly.domain.localdata.usecase.DeleteAllLocalData]) via
     * [DefaultTelemetryConsentRepository.resetConsent] — see that method's own KDoc for why this
     * alone does not erase telemetry data already transmitted before consent was revoked.
     */
    suspend fun clear() {
        runCatching { metadataDao.delete(METADATA_KEY) }
    }
}

private fun TelemetryConsent.toCached(): CachedTelemetryConsent = CachedTelemetryConsent(
    analytics = analytics.name,
    diagnostics = diagnostics.name,
)

/** An unrecognized stored value (a future enum rename, a corrupted row) falls back to [ConsentDecision.NotAsked] — never silently treated as [ConsentDecision.Granted]. */
private fun CachedTelemetryConsent.toTelemetryConsent(): TelemetryConsent = TelemetryConsent(
    analytics = analytics.toConsentDecisionOrNotAsked(),
    diagnostics = diagnostics.toConsentDecisionOrNotAsked(),
)

private fun String.toConsentDecisionOrNotAsked(): ConsentDecision =
    ConsentDecision.entries.firstOrNull { it.name == this } ?: ConsentDecision.NotAsked
