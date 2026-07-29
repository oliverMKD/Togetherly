package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.QuestSessionTransaction
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.validation.DomainValidationException

/**
 * An existing active session is never silently replaced — starting a new quest while one is
 * already active is a typed conflict. Replacing it deliberately is a separate, explicit policy
 * decision left for later, not this use case's default behavior. The conflict check and the write
 * happen atomically inside [questSessionTransaction] — this use case never does a plain read
 * followed by a later, separate write, which would leave a window for two concurrent starts to
 * both observe "no session" and one to silently clobber the other.
 *
 * The premium-access check below (Step 12.6) is a non-bypassable safety net, not the primary UX —
 * [PrepareQuestStartUseCase] is what the presentation layer calls *first* to decide whether to open
 * the paywall instead of ever reaching this class. This class re-checks anyway because it is the
 * actual session-creation boundary: "Do not rely only on a disabled button" (this feature's own
 * task spec) means the mutation itself must refuse, regardless of what any caller's UI state
 * believed was true.
 */
class StartQuest(
    private val familyRepository: FamilyRepository,
    private val questRepository: QuestRepository,
    private val questSessionTransaction: QuestSessionTransaction,
    private val entitlementRepository: EntitlementRepository,
    private val questAccessPolicy: QuestAccessPolicy,
    private val clock: AppClock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(questId: QuestId): DataResult<ActiveQuestSession> {
        val profileResult = familyRepository.getProfile()
        if (profileResult is DataResult.Error) return profileResult
        val profile = (profileResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))

        val questResult = questRepository.getQuest(questId)
        if (questResult is DataResult.Error) return questResult
        val quest = (questResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND))

        if (quest.access is QuestAccess.Premium) {
            val snapshot = currentAccessSnapshot()
            if (!questAccessPolicy.canAccess(quest.access, snapshot, clock.now())) {
                return DataResult.Error(AppError.Validation(ValidationError.PREMIUM_ACCESS_REQUIRED))
            }
        }

        val session = try {
            ActiveQuestSession(
                completionId = CompletionId(idGenerator.generate()),
                familyId = profile.id,
                questId = quest.id,
                questVersion = quest.version,
                startedAt = clock.now(),
            )
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        val startResult = questSessionTransaction.startActiveSession(session)
        if (startResult is DataResult.Error) return startResult

        return DataResult.Success(session)
    }

    /** A failed entitlement read never unlocks content — falls back to free/no-entitlements, never to "unknown means unlocked." */
    private suspend fun currentAccessSnapshot(): AccessSnapshot {
        val result = entitlementRepository.getAccess()
        return (result as? DataResult.Success)?.value ?: AccessSnapshot(FamilyAccess.free(), emptySet(), clock.now())
    }
}
