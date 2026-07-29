package com.togetherly.domain.localdata.usecase

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.notification.FakeReminderScheduler
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakePrivateMediaCommitter
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.family.repository.FakeFamilyDataCleaner
import com.togetherly.domain.family.repository.FamilyDataCleaner
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import com.togetherly.domain.telemetry.ConsentDecision
import com.togetherly.domain.telemetry.TelemetryConsent
import com.togetherly.domain.telemetry.repository.FakeTelemetryConsentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

private val TEST_PHOTO: MemoryMedia = MemoryMedia.Photo(id = MemoryMediaId("photo-1"), localReference = MediaReference("ref-photo"))
private val NOW = Instant.parse("2026-07-25T09:00:00Z")

class DeleteAllLocalDataTest {

    private fun useCase(
        completionRepository: FakeCompletionRepository = FakeCompletionRepository(),
        familyDataCleaner: FakeFamilyDataCleaner = FakeFamilyDataCleaner(),
        mediaCommitter: FakePrivateMediaCommitter = FakePrivateMediaCommitter(),
        reminderScheduler: FakeReminderScheduler = FakeReminderScheduler(),
        entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
        telemetryConsentRepository: FakeTelemetryConsentRepository = FakeTelemetryConsentRepository(),
    ) = DeleteAllLocalData(completionRepository, familyDataCleaner, mediaCommitter, reminderScheduler, entitlementRepository, telemetryConsentRepository)

    @Test
    fun successfulWipeCancelsRemindersDeletesFilesAndClearsEntitlementCache() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val familyDataCleaner = FakeFamilyDataCleaner()
        val mediaCommitter = FakePrivateMediaCommitter()
        val reminderScheduler = FakeReminderScheduler()
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val telemetryConsentRepository = FakeTelemetryConsentRepository(
            TelemetryConsent(analytics = ConsentDecision.Granted, diagnostics = ConsentDecision.Granted),
        )

        val result = useCase(completionRepository, familyDataCleaner, mediaCommitter, reminderScheduler, entitlementRepository, telemetryConsentRepository)()

        assertEquals(DataResult.Success(0), result)
        assertEquals(1, familyDataCleaner.deleteCallCount)
        assertEquals(1, reminderScheduler.cancelCallCount)
        assertEquals(listOf(TEST_PHOTO), mediaCommitter.deleteCommittedCalls)
        assertEquals(1, entitlementRepository.clearCacheCallCount)
        assertEquals(1, telemetryConsentRepository.resetConsentCallCount)
    }

    /** "Delete all local data" must reset consent to [ConsentDecision.NotAsked] — Step 14.1's own requirement. */
    @Test
    fun localDeletionResetsConsentToNotAsked() = runTest {
        val telemetryConsentRepository = FakeTelemetryConsentRepository(
            TelemetryConsent(analytics = ConsentDecision.Granted, diagnostics = ConsentDecision.Denied),
        )

        val result = useCase(telemetryConsentRepository = telemetryConsentRepository)()

        assertEquals(DataResult.Success(0), result)
        assertEquals(1, telemetryConsentRepository.resetConsentCallCount)
        assertEquals(TelemetryConsent.default(), telemetryConsentRepository.currentConsent)
    }

    /**
     * The database wipe is the one all-or-nothing gate — see [DeleteAllLocalData]'s own KDoc. If
     * it fails, nothing else (reminders, files, the RevenueCat-facing cache) may have already been
     * touched, so a family that hits this error still has everything exactly as before.
     */
    @Test
    fun databaseWipeFailureLeavesRemindersFilesAndEntitlementCacheUntouched() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val familyDataCleaner = FakeFamilyDataCleaner().apply { setNextError(AppError.Storage(StorageError.DELETE_FAILED)) }
        val mediaCommitter = FakePrivateMediaCommitter()
        val reminderScheduler = FakeReminderScheduler()
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW))
        val telemetryConsentRepository = FakeTelemetryConsentRepository()

        val result = useCase(completionRepository, familyDataCleaner, mediaCommitter, reminderScheduler, entitlementRepository, telemetryConsentRepository)()

        assertIs<DataResult.Error>(result)
        assertEquals(0, reminderScheduler.cancelCallCount)
        assertTrue(mediaCommitter.deleteCommittedCalls.isEmpty())
        assertEquals(0, entitlementRepository.clearCacheCallCount)
        assertEquals(0, telemetryConsentRepository.resetConsentCallCount)
    }

    @Test
    fun aFailedFileDeletionIsCountedButStillReportsOverallSuccess() = runTest {
        val completionRepository = FakeCompletionRepository().apply {
            saveCompletion(validQuestCompletion(media = listOf(TEST_PHOTO)))
        }
        val mediaCommitter = FakePrivateMediaCommitter().apply {
            setDeleteCommittedError(AppError.Storage(StorageError.DELETE_FAILED))
        }

        val result = useCase(completionRepository, mediaCommitter = mediaCommitter)()

        assertEquals(DataResult.Success(1), result)
    }

    @Test
    fun neverCallsAnythingBeyondClearCacheOnEntitlementRepository() = runTest {
        // FakeEntitlementRepository exposes no logOut-style call at all — EntitlementRepository's
        // own interface has none either (see that interface's own KDoc on the RevenueCat data
        // boundary). This test documents the intent: only clearCache is ever invoked.
        val entitlementRepository = FakeEntitlementRepository(AccessSnapshot(FamilyAccess.lifetime(), emptySet(), NOW))

        useCase(entitlementRepository = entitlementRepository)()

        assertEquals(1, entitlementRepository.clearCacheCallCount)
        assertEquals(0, entitlementRepository.restorePurchasesCallCount)
    }

    @Test
    fun withNothingStoredStillSucceedsWithZeroFailures() = runTest {
        assertEquals(DataResult.Success(0), useCase()())
    }

    @Test
    fun concurrentInvocationIsRejected() = runTest {
        val slowCleaner = object : FamilyDataCleaner {
            override suspend fun deleteAllFamilyData(): DataResult<Unit> {
                delay(50)
                return DataResult.Success(Unit)
            }
        }
        val deleteAllLocalData = DeleteAllLocalData(
            FakeCompletionRepository(),
            slowCleaner,
            FakePrivateMediaCommitter(),
            FakeReminderScheduler(),
            FakeEntitlementRepository(AccessSnapshot(FamilyAccess.free(), emptySet(), NOW)),
            FakeTelemetryConsentRepository(),
        )

        val results = listOf(
            async { deleteAllLocalData() },
            async { deleteAllLocalData() },
        ).awaitAll()

        assertEquals(1, results.count { it is DataResult.Error })
        val rejected = results.first { it is DataResult.Error } as DataResult.Error
        assertEquals(AppError.Validation(ValidationError.INVALID_STATE), rejected.error)
    }
}
