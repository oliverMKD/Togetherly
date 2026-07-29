package com.togetherly.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import org.junit.After
import org.junit.Before

/**
 * Shared setup for every Room DAO test: a fresh in-memory database per test, built through the
 * same [buildTogetherlyDatabase] production code path (driver, query dispatcher) so tests exercise
 * real construction, not a hand-rolled shortcut.
 *
 * These live under `androidDeviceTest` (instrumented), not `commonTest`/`androidHostTest`: Room's
 * Android database builder — including its in-memory variant — requires a real
 * [android.content.Context], and this project's JVM host test has no Robolectric configured, so
 * no working Context is obtainable there (see [com.togetherly.data.local.database.DatabaseMetadataInstrumentedTest]'s
 * own KDoc for the confirmed root cause).
 */
internal abstract class RoomDaoTest {

    protected lateinit var database: TogetherlyDatabase

    @Before
    fun setUpDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = buildTogetherlyDatabase(Room.inMemoryDatabaseBuilder(context, TogetherlyDatabase::class.java))
    }

    @After
    fun tearDownDatabase() {
        database.close()
    }
}
