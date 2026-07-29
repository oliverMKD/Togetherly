package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.core.id.SequentialIdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.repository.FakeQuestSessionTransaction
import com.togetherly.domain.completion.validActiveQuestSession
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.repository.FakeFamilyRepository
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

private fun validProfile() = FamilyProfile(
    id = FamilyId("family-1"),
    displayName = null,
    childAgeBands = setOf(AgeBand.AGE_6_TO_8),
    interests = setOf(QuestCategory.CREATE),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.SIMPLE_MATERIALS,
    reminderPreference = null,
    createdAt = NOW,
    updatedAt = NOW,
)

class ReplaceActiveQuestSessionTest {

    @Test
    fun replacesAnExistingActiveSessionUnconditionally() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-2"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val previous = validActiveQuestSession()
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(previous) }
        val questSessionTransaction = FakeQuestSessionTransaction(completionRepository)
        val useCase = ReplaceActiveQuestSession(
            familyRepository, questRepository, questSessionTransaction, TestAppClock(NOW), SequentialIdGenerator("completion"),
        )

        val result = useCase(quest.id)

        val session = (result as DataResult.Success).value
        assertEquals(quest.id, session.questId)
        assertEquals(DataResult.Success(session), completionRepository.getActiveSession())
    }

    @Test
    fun succeedsEvenWithNoPriorActiveSession() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val familyRepository = FakeFamilyRepository().apply { saveProfile(validProfile()) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val completionRepository = FakeCompletionRepository()
        val questSessionTransaction = FakeQuestSessionTransaction(completionRepository)
        val useCase = ReplaceActiveQuestSession(
            familyRepository, questRepository, questSessionTransaction, TestAppClock(NOW), SequentialIdGenerator("completion"),
        )

        val result = useCase(quest.id)

        val session = (result as DataResult.Success).value
        assertEquals(CompletionId("completion-0"), session.completionId)
    }
}
