package com.togetherly.app.di

import com.togetherly.content.repository.BundledQuestRepository
import com.togetherly.data.completion.RoomCompletionRepository
import com.togetherly.data.completion.RoomMemoryCleaner
import com.togetherly.data.completion.RoomQuestSessionTransaction
import com.togetherly.data.daily.RoomDailyQuestRepository
import com.togetherly.data.daily.RoomDailyQuestTransaction
import com.togetherly.data.family.RoomFamilyDataCleaner
import com.togetherly.data.family.RoomFamilyRepository
import com.togetherly.data.family.RoomFamilySettingsRepository
import com.togetherly.data.family.RoomQuestHistoryCleaner
import com.togetherly.data.journey.RoomJourneyRepository
import com.togetherly.data.media.PrivateMediaCommitterImpl
import com.togetherly.data.purchase.DefaultRevenueCatDataSource
import com.togetherly.data.purchase.EntitlementCache
import com.togetherly.data.purchase.RevenueCatCustomerAttributesRepository
import com.togetherly.data.purchase.RevenueCatDataSource
import com.togetherly.data.purchase.RevenueCatEntitlementRepository
import com.togetherly.data.saved.RoomSavedQuestRepository
import com.togetherly.data.telemetry.DefaultTelemetryConsentRepository
import com.togetherly.data.telemetry.TelemetryConsentCache
import com.togetherly.domain.completion.repository.CompletionRepository
import com.togetherly.domain.completion.repository.PrivateMediaCommitter
import com.togetherly.domain.completion.repository.QuestSessionTransaction
import com.togetherly.domain.daily.repository.DailyQuestRepository
import com.togetherly.domain.daily.repository.DailyQuestTransaction
import com.togetherly.domain.completion.repository.MemoryCleaner
import com.togetherly.domain.family.repository.FamilyDataCleaner
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.family.repository.FamilySettingsRepository
import com.togetherly.domain.family.repository.QuestHistoryCleaner
import com.togetherly.domain.journey.repository.JourneyRepository
import com.togetherly.domain.purchase.repository.CustomerAttributesRepository
import com.togetherly.domain.purchase.repository.EntitlementRepository
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.saved.repository.SavedQuestRepository
import com.togetherly.domain.telemetry.repository.TelemetryConsentRepository
import org.koin.dsl.module

/**
 * Production repository bindings. [QuestRepository], [FamilyRepository], [DailyQuestRepository],
 * [SavedQuestRepository], [CompletionRepository], [JourneyRepository], [QuestSessionTransaction],
 * [FamilyDataCleaner] and — since Step 11 — [EntitlementRepository] (RevenueCat-backed) all have
 * real implementations; production must never resolve any of their `Fake*` test doubles.
 *
 * [QuestSessionTransaction], [DailyQuestTransaction] and [FamilyDataCleaner] never expose their
 * `Room*` implementation type outside this module — every consumer (use cases, and any future
 * graph test) depends only on the domain interface. [RevenueCatDataSource] is the one exception
 * exposed as its own bindable type (not just folded into [EntitlementRepository]'s `single {}`)
 * since nothing else in this module follows a data-source/repository split — it exists purely so
 * [RevenueCatEntitlementRepository]'s own tests (and any future consumer needing the raw provider
 * boundary) can depend on the interface rather than reaching into the repository's internals.
 */
val repositoryModule = module {
    single<QuestRepository> {
        BundledQuestRepository(
            catalogueLoader = get(),
            dispatchers = get(),
        )
    }

    single<FamilyRepository> {
        RoomFamilyRepository(
            familyDao = get(),
            familyMapper = get(),
            database = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<FamilySettingsRepository> {
        RoomFamilySettingsRepository(
            familyDao = get(),
            familyMapper = get(),
            clock = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<DailyQuestRepository> {
        RoomDailyQuestRepository(
            dailyQuestDao = get(),
            dailyQuestMapper = get(),
            dismissedQuestMapper = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<SavedQuestRepository> {
        RoomSavedQuestRepository(
            savedQuestDao = get(),
            mapper = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<CompletionRepository> {
        RoomCompletionRepository(
            completionDao = get(),
            activeQuestSessionMapper = get(),
            completionMapper = get(),
            database = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<JourneyRepository> {
        RoomJourneyRepository(
            completionDao = get(),
            questRepository = get(),
            completionMapper = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<DailyQuestTransaction> {
        RoomDailyQuestTransaction(
            dailyQuestDao = get(),
            dailyQuestMapper = get(),
            dismissedQuestMapper = get(),
            database = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<QuestSessionTransaction> {
        RoomQuestSessionTransaction(
            completionDao = get(),
            activeQuestSessionMapper = get(),
            completionMapper = get(),
            database = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<FamilyDataCleaner> {
        RoomFamilyDataCleaner(
            familyDao = get(),
            dailyQuestDao = get(),
            savedQuestDao = get(),
            completionDao = get(),
            database = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<PrivateMediaCommitter> {
        PrivateMediaCommitterImpl(mediaStorage = get())
    }

    single<MemoryCleaner> {
        RoomMemoryCleaner(
            completionDao = get(),
            database = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<QuestHistoryCleaner> {
        RoomQuestHistoryCleaner(
            dailyQuestDao = get(),
            completionDao = get(),
            database = get(),
            dispatchers = get(),
            diagnostics = get(),
        )
    }

    single<RevenueCatDataSource> {
        DefaultRevenueCatDataSource(
            configurator = get(),
            clock = get(),
            logger = get(),
            diagnostics = get(),
        )
    }

    single {
        EntitlementCache(metadataDao = get())
    }

    single<EntitlementRepository> {
        RevenueCatEntitlementRepository(
            dataSource = get(),
            cache = get(),
            clock = get(),
            dispatchers = get(),
        )
    }

    single<CustomerAttributesRepository> {
        RevenueCatCustomerAttributesRepository(
            dataSource = get(),
            consentRepository = get(),
            logger = get(),
        )
    }

    single {
        TelemetryConsentCache(metadataDao = get())
    }

    single<TelemetryConsentRepository> {
        DefaultTelemetryConsentRepository(
            cache = get(),
            logger = get(),
            dispatchers = get(),
        )
    }
}
