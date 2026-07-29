package com.togetherly.data.local.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.togetherly.data.local.RoomDaoTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A Room smoke test, not a full suite — proves the generated Room code, the bundled SQLite driver
 * and the metadata DAO work together end to end.
 *
 * This runs as an instrumented test rather than a `commonTest`/`androidHostTest` one because
 * Room's Android database builder (including its in-memory variant) requires a real
 * [android.content.Context]. `:shared:testAndroidHostTest` is a plain JVM host with no Robolectric
 * configured in this project — the Android stub jar throws `RuntimeException("Stub!")` the moment
 * any real method is called on it (confirmed independently in the content-resource work, Step
 * 5.5), so no working Context is obtainable there. A real device/emulator is required to execute
 * this test; it was written and reviewed for correctness but could not be run in this sandboxed
 * environment (see the final Step 6.1 report).
 */
@RunWith(AndroidJUnit4::class)
internal class DatabaseMetadataInstrumentedTest : RoomDaoTest() {

    @Test
    fun insertReadReplaceAndDeleteMetadataRoundTrips() = runTest {
        val dao = database.metadataDao()

        assertNull(dao.getValue("greeting"))

        dao.set(DatabaseMetadataEntity(key = "greeting", value = "hello"))
        assertEquals("hello", dao.getValue("greeting"))

        dao.set(DatabaseMetadataEntity(key = "greeting", value = "hi"))
        assertEquals("hi", dao.getValue("greeting"))

        dao.delete("greeting")
        assertNull(dao.getValue("greeting"))
    }
}
