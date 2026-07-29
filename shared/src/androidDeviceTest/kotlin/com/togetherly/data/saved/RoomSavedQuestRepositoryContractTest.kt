package com.togetherly.data.saved

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.core.coroutines.TestAppDispatchers
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.mapper.SavedQuestMapper
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.saved.repository.SavedQuestRepositoryContractTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class RoomSavedQuestRepositoryContractTest : SavedQuestRepositoryContractTest() {

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

    override fun repository(): SavedQuestRepository = RoomSavedQuestRepository(
        savedQuestDao = database.savedQuestDao(),
        mapper = SavedQuestMapper(),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher()),
    )
}
