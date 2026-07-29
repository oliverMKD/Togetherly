package com.togetherly.domain.localdata.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.notification.ReminderScheduler
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.PrivateMediaCommitter
import com.togetherly.domain.family.repository.FamilyDataCleaner
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.telemetry.repository.TelemetryConsentRepository
import kotlinx.coroutines.sync.Mutex

/**
 * "Delete all local data" (Step 13.7) — the one application-level coordinator for a full local
 * wipe. Never called from a ViewModel's own multi-repository logic; a ViewModel only ever calls
 * this single entry point and reacts to its result.
 *
 * Ordering (deliberately not the checklist order in this feature's own task spec, which lists
 * "cancel reminders"/"delete media" before the database steps — this coordinator wipes the
 * database *first*, as the one all-or-nothing, transactional, critical step, and only then
 * performs the remaining best-effort cleanup):
 *
 * 1. [completionRepository.getCompletions] — reads every completion's media *before* anything is
 *    deleted, so the files can still be found afterward (same reasoning as [DeleteMemories]).
 * 2. [familyDataCleaner.deleteAllFamilyData] — one atomic transaction: family profile + every
 *    preference row, daily selections, dismissals, saved quests, the active session, and every
 *    completion (with cascading reaction/media metadata). If this fails, everything below is
 *    skipped and the failure is returned immediately — nothing else has touched real state yet,
 *    so a family that hits this error still has every reminder, file, and cached entitlement
 *    exactly as before. This is what "never claim deletion succeeded if critical data remains"
 *    means here: the database wipe is the gate every other step depends on.
 * 3. Only once step 2 succeeds, each of the following runs best-effort (a failure here is logged
 *    conceptually but never turns an already-successful database wipe into a reported failure —
 *    the data this action's name promises to delete is unconditionally already gone by this
 *    point):
 *    - [reminderScheduler.cancel] — nothing left to remind a family about once their profile and
 *      preferences are gone.
 *    - Every media file read in step 1, deleted via [mediaCommitter] — same "database row first,
 *      file best-effort after" ordering [DeleteMemories] and
 *      [com.togetherly.domain.completion.usecase.DeleteCompletion] already establish.
 *    - [entitlementRepository.clearCache] — clears only the *locally cached* entitlement snapshot;
 *      see that method's own KDoc for the RevenueCat data boundary this must never cross (no
 *      subscription cancellation, no server-side change, no `logOut()` — this app's RevenueCat
 *      identity is anonymous-only and never migrated, see
 *      [com.togetherly.data.purchase.RevenueCatConfigurator]'s own KDoc). A still-genuinely-
 *      entitled family sees Family Plus access reappear automatically once the provider
 *      reconciles again — recoverable through RevenueCat/store restore behavior, never through
 *      anything this coordinator does itself.
 *    - [telemetryConsentRepository.resetConsent] (Step 14.1) — resets both analytics and
 *      diagnostics consent back to [com.togetherly.domain.telemetry.ConsentDecision.NotAsked] and
 *      clears the locally persisted consent row. [com.togetherly.core.telemetry.TelemetryCoordinator]
 *      (already subscribed to [TelemetryConsentRepository.observeConsent] for the whole app
 *      process) reacts to that transition the same way it reacts to a family manually revoking —
 *      disabling both providers and resetting their state — so this use case never touches
 *      `ProductAnalytics`/`OperationalDiagnostics` itself. This does **not** erase analytics events
 *      already transmitted before this call, if any were ever sent while consent was granted — see
 *      `docs/telemetry.md` for that distinction; only the on-device consent record and provider
 *      state are Togetherly's own to erase.
 *
 * No explicit "recreate database defaults" step exists here: nothing in this schema needs manual
 * reseeding after a wipe. [com.togetherly.data.local.database.DatabaseMetadataEntity]'s schema/
 * migration bookkeeping rows are never touched by [familyDataCleaner] (see that interface's own
 * KDoc) or by this coordinator beyond the one entitlement-cache key [entitlementRepository.clearCache]
 * clears — Room's own schema stays intact. A fresh family profile (and everything that depends on
 * one existing) is created the same way it always is: by completing onboarding again, exactly like
 * a first install.
 *
 * "Clear onboarding state" has no dedicated step either — there is no stored onboarding-complete
 * flag to clear (see [com.togetherly.navigation.state.BootstrapViewModel]'s own KDoc: onboarding
 * status is derived purely from whether a family profile exists). Deleting the profile in step 2
 * *is* clearing onboarding state.
 *
 * "Stop active session writes" has no separate mechanism beyond [mutex] and the database wipe
 * itself: this is a local, single-process app with no background writer that could race a
 * deletion outside of what the deleting ViewModel's own busy-state (and this coordinator's mutex)
 * already blocks — [FamilyDataCleaner] deletes the active session row as part of its own atomic
 * transaction, so no quest-mode write can land "in between."
 *
 * Never navigates anywhere itself — returning [DataResult.Success] is the caller's (a ViewModel's)
 * signal that it is now safe to navigate to onboarding, clearing the whole back stack. This
 * coordinator has no navigation dependency at all.
 */
class DeleteAllLocalData(
    private val completionRepository: CompletionRepository,
    private val familyDataCleaner: FamilyDataCleaner,
    private val mediaCommitter: PrivateMediaCommitter,
    private val reminderScheduler: ReminderScheduler,
    private val entitlementRepository: EntitlementRepository,
    private val telemetryConsentRepository: TelemetryConsentRepository,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): DataResult<Int> {
        if (!mutex.tryLock()) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_STATE))
        }
        try {
            val existing = when (val result = completionRepository.getCompletions()) {
                is DataResult.Error -> return result
                is DataResult.Success -> result.value
            }
            val mediaToDelete = existing.flatMap { it.media }

            val wipeResult = familyDataCleaner.deleteAllFamilyData()
            if (wipeResult is DataResult.Error) return wipeResult

            reminderScheduler.cancel()

            var failedFileDeletions = 0
            mediaToDelete.forEach { media ->
                if (mediaCommitter.deleteCommitted(media) is DataResult.Error) failedFileDeletions++
            }

            entitlementRepository.clearCache()
            telemetryConsentRepository.resetConsent()

            return DataResult.Success(failedFileDeletions)
        } finally {
            mutex.unlock()
        }
    }
}
