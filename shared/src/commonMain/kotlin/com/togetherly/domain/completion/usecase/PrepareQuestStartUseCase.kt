package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.QuestRepository

/**
 * The one place every route into Quest Mode is required to check before starting — Today, Explore,
 * Pack Details, Saved quests, a deep link, or restored navigation state all funnel through
 * [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel.onAction]'s own
 * `StartClicked` handling, which calls this *before* [StartQuest], and [StartQuest] itself performs
 * the same check again as a non-bypassable safety net (see that class's own KDoc) — this use case
 * exists so the presentation layer has a semantic *decision* to act on (open the paywall vs.
 * proceed) rather than only a boolean a caller could forget to check or a stale UI flag could get
 * wrong.
 *
 * A failed entitlement read never unlocks content — same fallback-to-free-access reasoning as
 * [com.togetherly.domain.explore.usecase.EvaluateQuestAccessUseCase]'s own KDoc.
 */
class PrepareQuestStartUseCase(
    private val questRepository: QuestRepository,
    private val entitlementRepository: EntitlementRepository,
    private val questAccessPolicy: QuestAccessPolicy,
    private val clock: AppClock,
) {
    suspend operator fun invoke(questId: QuestId): PrepareQuestStartResult {
        val questResult = questRepository.getQuest(questId)
        val quest = (questResult as? DataResult.Success)?.value ?: return PrepareQuestStartResult.NotFound

        if (quest.access is QuestAccess.Free) return PrepareQuestStartResult.Allowed(quest)

        val snapshot = currentAccessSnapshot()
        return if (questAccessPolicy.canAccess(quest.access, snapshot, clock.now())) {
            PrepareQuestStartResult.Allowed(quest)
        } else {
            PrepareQuestStartResult.RequiresFamilyPlus(questId)
        }
    }

    private suspend fun currentAccessSnapshot(): AccessSnapshot {
        val result = entitlementRepository.getAccess()
        return (result as? DataResult.Success)?.value ?: AccessSnapshot(FamilyAccess.free(), emptySet(), clock.now())
    }
}

/** Provider-neutral — never a RevenueCat type, never a raw [com.togetherly.core.error.AppError] the UI would have to interpret. */
sealed interface PrepareQuestStartResult {
    data class Allowed(val quest: FamilyQuest) : PrepareQuestStartResult
    data class RequiresFamilyPlus(val questId: QuestId) : PrepareQuestStartResult
    data object NotFound : PrepareQuestStartResult
}
