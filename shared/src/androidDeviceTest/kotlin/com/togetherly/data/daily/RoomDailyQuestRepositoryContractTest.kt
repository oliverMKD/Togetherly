package com.togetherly.data.daily

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.core.telemetry.FakeOperationalDiagnostics
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.DailyQuestMapper
import com.togetherly.data.local.mapper.DismissedQuestMapper
import com.togetherly.domain.daily.repository.DailyQuestRepository
import com.togetherly.domain.daily.repository.DailyQuestRepositoryContractTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class RoomDailyQuestRepositoryContractTest : DailyQuestRepositoryContractTest() {

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

    override fun repository(): DailyQuestRepository = RoomDailyQuestRepository(
        dailyQuestDao = database.dailyQuestDao(),
        dailyQuestMapper = DailyQuestMapper(),
        dismissedQuestMapper = DismissedQuestMapper(),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
        diagnostics = FakeOperationalDiagnostics(),
    )
}
