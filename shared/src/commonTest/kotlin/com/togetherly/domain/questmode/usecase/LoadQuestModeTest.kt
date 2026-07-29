package com.togetherly.domain.questmode.usecase

import com.togetherly.core.datetime.TestAppClock
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.validActiveQuestSession
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.FakeQuestRepository
import com.togetherly.domain.quest.validFamilyQuest
import com.togetherly.domain.questmode.LoadQuestModeResult
import com.togetherly.domain.questmode.QuestTimerPolicy
import com.togetherly.domain.questmode.QuestTimerState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-06-15T08:00:00Z")

class LoadQuestModeTest {

    @Test
    fun noActiveSessionIsReported() = runTest {
        val useCase = LoadQuestMode(FakeCompletionRepository(), FakeQuestRepository(), QuestTimerPolicy(), TestAppClock(NOW))

        val result = useCase(CompletionId("completion-1"))

        assertEquals(LoadQuestModeResult.NoActiveSession, result)
    }

    @Test
    fun completionIdMismatchIsReported() = runTest {
        val session = validActiveQuestSession(completionId = CompletionId("completion-current"))
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val useCase = LoadQuestMode(completionRepository, FakeQuestRepository(), QuestTimerPolicy(), TestAppClock(NOW))

        val result = useCase(CompletionId("completion-stale"))

        assertEquals(LoadQuestModeResult.SessionMismatch, result)
    }

    @Test
    fun missingQuestIsReported() = runTest {
        val session = validActiveQuestSession(completionId = CompletionId("completion-1"), questId = QuestId("quest-1"))
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val useCase = LoadQuestMode(completionRepository, FakeQuestRepository(), QuestTimerPolicy(), TestAppClock(NOW))

        val result = useCase(CompletionId("completion-1"))

        assertEquals(LoadQuestModeResult.QuestUnavailable, result)
    }

    @Test
    fun validSessionLoadsSuccessfully() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val session = validActiveQuestSession(completionId = CompletionId("completion-1"), questId = quest.id, startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val useCase = LoadQuestMode(completionRepository, questRepository, QuestTimerPolicy(), TestAppClock(NOW))

        val result = useCase(CompletionId("completion-1"))

        val success = result as LoadQuestModeResult.Success
        assertEquals(quest, success.session.quest)
        assertEquals(session, success.session.activeSession)
        assertEquals(QuestTimerState.NotRequired, success.session.timerState)
    }

    @Test
    fun loadingDoesNotAbandonOrCompleteAnything() = runTest {
        val quest = validFamilyQuest(id = QuestId("quest-1"))
        val session = validActiveQuestSession(completionId = CompletionId("completion-1"), questId = quest.id, startedAt = NOW)
        val completionRepository = FakeCompletionRepository().apply { saveActiveSession(session) }
        val questRepository = FakeQuestRepository().apply { setQuests(listOf(quest)) }
        val useCase = LoadQuestMode(completionRepository, questRepository, QuestTimerPolicy(), TestAppClock(NOW))

        useCase(CompletionId("completion-1"))

        val activeSessionResult = completionRepository.getActiveSession()
        val activeSession = (activeSessionResult as com.togetherly.core.result.DataResult.Success).value
        assertTrue(activeSession == session)
    }
}
