package com.togetherly.content.repository

import com.togetherly.content.loader.QuestCatalogueLoader
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.result.DataResult
import com.togetherly.core.result.map
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPack
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.repository.QuestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads the bundled catalogue lazily on first access rather than at application startup — this
 * keeps startup independent of content I/O. [QuestCatalogueLoader] already caches a successful
 * load internally, so repeated calls here never re-read or re-parse; [catalogueState] additionally
 * makes the `observe*` methods a hot, never-completing stream that every collector shares,
 * populated the first time any method is called instead of sitting empty forever.
 *
 * A successful load is converted once into an [IndexedQuestCatalogue] and cached in that indexed
 * shape — quest and pack lookups are O(1) map reads, never a linear scan, and packs are sorted by
 * [QuestPack.sortOrder] exactly once rather than on every call.
 *
 * [catalogueState] holds the outcome of the most recent load attempt, success or failure, so
 * observers always have something to react to. Only a [DataResult.Success] short-circuits future
 * loads — a cached [DataResult.Error] is never treated as a reason to skip retrying, so a failed
 * load can always be retried on the next call.
 */
internal class BundledQuestRepository(
    private val catalogueLoader: QuestCatalogueLoader,
    private val dispatchers: AppDispatchers,
) : QuestRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val catalogueState = MutableStateFlow<DataResult<IndexedQuestCatalogue>?>(null)

    private suspend fun ensureLoaded(): DataResult<IndexedQuestCatalogue> {
        val current = catalogueState.value
        if (current is DataResult.Success) return current

        return withContext(dispatchers.io) { catalogueLoader.load() }
            .map { it.indexed() }
            .also { result -> catalogueState.value = result }
    }

    private fun ensureLoadStarted() {
        if (catalogueState.value !is DataResult.Success) {
            scope.launch { ensureLoaded() }
        }
    }

    override fun observeAllQuests(): Flow<DataResult<List<FamilyQuest>>> {
        ensureLoadStarted()
        return catalogueState.filterNotNull().map { result -> result.map { it.quests } }
    }

    override fun observeQuest(questId: QuestId): Flow<DataResult<FamilyQuest?>> {
        ensureLoadStarted()
        return catalogueState.filterNotNull().map { result ->
            result.map { catalogue -> catalogue.questsById[questId] }
        }
    }

    override suspend fun getAllQuests(): DataResult<List<FamilyQuest>> =
        ensureLoaded().map { it.quests }

    override suspend fun getQuest(questId: QuestId): DataResult<FamilyQuest?> =
        ensureLoaded().map { it.questsById[questId] }

    override fun observePacks(): Flow<DataResult<List<QuestPack>>> {
        ensureLoadStarted()
        return catalogueState.filterNotNull().map { result -> result.map { it.packs } }
    }

    override suspend fun getPacks(): DataResult<List<QuestPack>> =
        ensureLoaded().map { it.packs }

    override suspend fun getPack(packId: QuestPackId): DataResult<QuestPack?> =
        ensureLoaded().map { it.packsById[packId] }
}
