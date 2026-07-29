package com.togetherly.app.di

import androidx.room.RoomDatabase
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.data.local.completion.CompletionDao
import com.togetherly.data.local.daily.DailyQuestDao
import com.togetherly.data.local.database.DatabaseMetadataDao
import com.togetherly.data.local.database.TogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabase
import com.togetherly.data.local.database.buildTogetherlyDatabaseCapturingFailures
import com.togetherly.data.local.database.createDatabaseBuilder
import com.togetherly.data.local.family.FamilyDao
import com.togetherly.data.local.mapper.ActiveQuestSessionMapper
import com.togetherly.data.local.mapper.DailyQuestMapper
import com.togetherly.data.local.mapper.DismissedQuestMapper
import com.togetherly.data.local.mapper.FamilyProfileMapper
import com.togetherly.data.local.mapper.QuestCompletionMapper
import com.togetherly.data.local.mapper.SavedQuestMapper
import com.togetherly.data.local.saved.SavedQuestDao
import org.koin.dsl.module

/**
 * Registers the database foundation, its DAOs, and the persistence/domain mappers (Steps
 * 6.1-6.3) — all singleton-scoped, so [TogetherlyDatabase] is constructed at most once per
 * process, lazily on first resolution. Repository bindings live in [repositoryModule].
 */
val databaseModule = module {
    single<RoomDatabase.Builder<TogetherlyDatabase>> { createDatabaseBuilder() }
    single<TogetherlyDatabase> {
        buildTogetherlyDatabaseCapturingFailures(diagnostics = get<OperationalDiagnostics>()) { buildTogetherlyDatabase(get()) }
    }
    single<DatabaseMetadataDao> { get<TogetherlyDatabase>().metadataDao() }
    single<FamilyDao> { get<TogetherlyDatabase>().familyDao() }
    single<DailyQuestDao> { get<TogetherlyDatabase>().dailyQuestDao() }
    single<SavedQuestDao> { get<TogetherlyDatabase>().savedQuestDao() }
    single<CompletionDao> { get<TogetherlyDatabase>().completionDao() }

    single { FamilyProfileMapper() }
    single { DailyQuestMapper() }
    single { DismissedQuestMapper() }
    single { SavedQuestMapper() }
    single { ActiveQuestSessionMapper() }
    single { QuestCompletionMapper() }
}
