package com.togetherly.domain.explore.usecase

import com.togetherly.core.result.DataResult
import com.togetherly.domain.explore.QuestPackDetail
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.quest.validQuestPack
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetQuestPackUseCaseTest {

    @Test
    fun resolvesThePackAndItsMemberQuests() = runTest {
        val questOne = validFamilyQuest(id = QuestId("quest-1"))
        val questTwo = validFamilyQuest(id = QuestId("quest-2"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(questOne.id, questTwo.id))
        val questRepository = FakeQuestRepository().apply {
            setQuests(listOf(questOne, questTwo))
            setPacks(listOf(pack))
        }
        val useCase = GetQuestPackUseCase(questRepository)

        val result = useCase(pack.id)

        assertEquals(DataResult.Success(QuestPackDetail(pack = pack, quests = listOf(questOne, questTwo))), result)
    }

    @Test
    fun aStaleQuestReferenceIsSkippedRatherThanFailingTheWholePack() = runTest {
        val questOne = validFamilyQuest(id = QuestId("quest-1"))
        val pack = validQuestPack(id = QuestPackId("pack-1"), questIds = listOf(questOne.id, QuestId("removed-quest")))
        val questRepository = FakeQuestRepository().apply {
            setQuests(listOf(questOne))
            setPacks(listOf(pack))
        }
        val useCase = GetQuestPackUseCase(questRepository)

        val result = useCase(pack.id) as DataResult.Success

        assertEquals(listOf(questOne), result.value?.quests)
    }

    @Test
    fun missingPackReturnsSuccessNull() = runTest {
        val questRepository = FakeQuestRepository()
        val useCase = GetQuestPackUseCase(questRepository)

        val result = useCase(QuestPackId("missing"))

        assertEquals(DataResult.Success(null), result)
        assertNull((result as DataResult.Success).value)
    }
}
