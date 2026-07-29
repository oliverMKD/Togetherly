package com.togetherly.domain.explore.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestAccess

/**
 * Returns `true` when the quest is currently accessible (same polarity as
 * [QuestAccessPolicy.canAccess], which this wraps) — callers building an
 * [com.togetherly.domain.explore.ExploreQuestItem.locked] flag negate it, they don't re-derive it.
 * The same locked-quest reasoning [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel]
 * already applies inline, extracted here so Explore's catalogue grid (evaluating many quests at
 * once, not just the one quest a detail screen shows) doesn't have to duplicate it. A failed
 * entitlement read never unlocks content — it falls back to free/no-entitlements, same as
 * [com.togetherly.feature.questdetail.presentation.QuestDetailViewModel]'s own `currentAccessSnapshot`.
 */
class EvaluateQuestAccessUseCase(
    private val entitlementRepository: EntitlementRepository,
    private val questAccessPolicy: QuestAccessPolicy,
    private val clock: AppClock,
) {
    suspend operator fun invoke(quest: FamilyQuest): Boolean {
        if (quest.access is QuestAccess.Free) return true

        val snapshot = currentAccessSnapshot()
        return questAccessPolicy.canAccess(quest.access, snapshot, clock.now())
    }

    private suspend fun currentAccessSnapshot(): AccessSnapshot {
        val result = entitlementRepository.getAccess()
        return (result as? DataResult.Success)?.value ?: AccessSnapshot(FamilyAccess.free(), emptySet(), clock.now())
    }
}
