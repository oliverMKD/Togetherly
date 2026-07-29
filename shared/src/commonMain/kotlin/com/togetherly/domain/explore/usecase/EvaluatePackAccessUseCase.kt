package com.togetherly.domain.explore.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.result.DataResult
import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.QuestAccessPolicy
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestPack

/**
 * Same shape and polarity as [EvaluateQuestAccessUseCase] (`true` = currently accessible), applied
 * to [QuestPack.access] instead of a single quest's — kept as its own class rather than one
 * overloaded use case, since a pack and a quest are different domain types with no shared
 * supertype [QuestAccessPolicy.canAccess] could dispatch on generically.
 */
class EvaluatePackAccessUseCase(
    private val entitlementRepository: EntitlementRepository,
    private val questAccessPolicy: QuestAccessPolicy,
    private val clock: AppClock,
) {
    suspend operator fun invoke(pack: QuestPack): Boolean {
        if (pack.access is QuestAccess.Free) return true

        val snapshot = currentAccessSnapshot()
        return questAccessPolicy.canAccess(pack.access, snapshot, clock.now())
    }

    private suspend fun currentAccessSnapshot(): AccessSnapshot {
        val result = entitlementRepository.getAccess()
        return (result as? DataResult.Success)?.value ?: AccessSnapshot(FamilyAccess.free(), emptySet(), clock.now())
    }
}
