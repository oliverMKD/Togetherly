package com.togetherly.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.togetherly.core.coroutines.ioDispatcher
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics

/**
 * Platform-specific: resolves wherever this platform keeps private application data and returns
 * an unconfigured builder pointed at it. Never exposes that path to callers — only the builder.
 */
internal expect fun createDatabaseBuilder(): RoomDatabase.Builder<TogetherlyDatabase>

/**
 * The bundled SQLite driver is used on every platform (including Android) so database behavior
 * stays identical across targets rather than depending on the OS-provided SQLite version. Queries
 * run on [ioDispatcher] — the same background dispatcher [com.togetherly.core.coroutines.AppDispatchers]
 * uses elsewhere — never the main thread. Still no destructive-migration fallback configured: a
 * missing migration path must fail loudly, never silently delete a family's data (see
 * [TogetherlyDatabase]'s own KDoc). [MIGRATION_1_2] (Step 13.1) is the first real migration this
 * database has ever needed.
 */
internal fun buildTogetherlyDatabase(
    builder: RoomDatabase.Builder<TogetherlyDatabase>,
): TogetherlyDatabase = builder
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(ioDispatcher)
    .build()

/**
 * The "database migration failure" capture boundary (Step 14.5), pulled out of `app/di/DatabaseModule.kt`'s
 * own Koin lambda so it's directly unit-testable without spinning up Room at all — [build] is
 * `buildTogetherlyDatabase` in production, a throwing/succeeding fake in tests. A migration failure
 * must still surface loudly (never silently drop a family's data — see [TogetherlyDatabase]'s own
 * KDoc): [throwable] is always rethrown after being reported, never swallowed.
 */
internal fun <T> buildTogetherlyDatabaseCapturingFailures(
    diagnostics: OperationalDiagnostics,
    build: () -> T,
): T = try {
    build()
} catch (throwable: Throwable) {
    diagnostics.captureHandledException(throwable, DiagnosticContext(mapOf("feature" to "database", "operation" to "migration")))
    throw throwable
}
