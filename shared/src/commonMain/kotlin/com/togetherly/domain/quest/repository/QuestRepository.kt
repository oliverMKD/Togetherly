package com.togetherly.domain.quest.repository

import com.togetherly.core.result.DataResult
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.quest.QuestPackId
import kotlinx.coroutines.flow.Flow

/**
 * Read-only access to the curated quest catalogue and its packs.
 *
 * Observable methods (`observe*`) support UI and other long-lived state; one-shot `suspend`
 * methods support orchestration and future recommendation use cases that just need a snapshot.
 * Both are kept deliberately, rather than collapsing to one shape — a use case computing a
 * recommendation shouldn't have to stay subscribed to a Flow just to read the catalogue once.
 *
 * A missing quest or pack is `Success(null)`, never an error — only a genuine read failure is
 * [DataResult.Error]. Quest ordering must be deterministic across calls for the same underlying
 * data; pack ordering follows [QuestPack.sortOrder]. This repository returns domain models only
 * (never DTOs or database entities), and performs no premium-access filtering, no recommendation
 * scoring, and no UI label or artwork resolution — those belong to policies and use cases.
 */
interface QuestRepository {

    fun observeAllQuests(): Flow<DataResult<List<FamilyQuest>>>

    fun observeQuest(
        questId: QuestId,
    ): Flow<DataResult<FamilyQuest?>>

    suspend fun getAllQuests(): DataResult<List<FamilyQuest>>

    suspend fun getQuest(
        questId: QuestId,
    ): DataResult<FamilyQuest?>

    fun observePacks(): Flow<DataResult<List<QuestPack>>>

    suspend fun getPacks(): DataResult<List<QuestPack>>

    suspend fun getPack(
        packId: QuestPackId,
    ): DataResult<QuestPack?>
}
