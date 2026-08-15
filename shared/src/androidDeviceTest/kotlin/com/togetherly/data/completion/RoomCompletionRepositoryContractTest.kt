package com.togetherly.data.completion

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.data.local.completion.ACTIVE_QUEST_SESSION_SLOT_ID
import com.togetherly.data.local.completion.ActiveQuestSessionEntity
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.CompletionRepositoryContractTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class RoomCompletionRepositoryContractTest : CompletionRepositoryContractTest() {

    private lateinit var database: TogetherlyDatabase

    @Before
    fun setUpDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = buildTogetherlyDatabase(Room.inMemoryDatabaseBuilder(context, TogetherlyDatabase::class.java))
    }

    @After
    fun tearDownDatabase() {
        database.close()
    }

    override fun repository(): CompletionRepository = RoomCompletionRepository(
        completionDao = database.completionDao(),
        activeQuestSessionMapper = ActiveQuestSessionMapper(),
        completionMapper = QuestCompletionMapper(),
        database = database,
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        diagnostics = FakeOperationalDiagnostics(),
    )

    /**
     * A blank [ActiveQuestSessionEntity.completionId] can never come from
     * [RoomCompletionRepository.saveActiveSession] (the domain [com.togetherly.domain.completion.CompletionId]
     * value class already rejects it) — this writes the corrupted row directly through the DAO to
     * prove the read path still surfaces it as a typed error instead of throwing or silently
     * dropping the row. Only exercisable against Room: [FakeCompletionRepository] has no
     * validation-bypassing write path at all.
     */
    @Test
    fun corruptedActiveSessionRowReturnsTypedStorageError() = runTest {
        database.completionDao().insertActiveSession(
            ActiveQuestSessionEntity(
                slotId = ACTIVE_QUEST_SESSION_SLOT_ID,
                completionId = "",
                familyId = "family-1",
                questId = "quest-1",
                questVersion = 1,
                startedAtEpochMillis = 1_000L,
            ),
        )

        val result = repository().getActiveSession()

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), result)
    }
}
