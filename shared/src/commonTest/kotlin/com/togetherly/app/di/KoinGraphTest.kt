package com.togetherly.app.di

import com.togetherly.app.application.AppConfiguration
import com.togetherly.content.loader.QuestCatalogueLoader
import com.togetherly.content.mapper.FamilyQuestMapper
import com.togetherly.content.mapper.QuestCatalogueMapper
import com.togetherly.content.mapper.QuestPackMapper
import com.togetherly.content.resource.QuestCatalogueResource
import com.togetherly.content.schema.QuestCatalogueParser
import com.togetherly.content.validation.QuestCatalogueValidator
import com.togetherly.core.coroutines.AppDispatchers
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.id.IdGenerator
import com.togetherly.data.telemetry.SentryDsnProvider
import com.togetherly.domain.quest.repository.QuestRepository
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test

class KoinGraphTest {

    private var koinApp: KoinApplication? = null

    @AfterTest
    fun tearDown() {
        koinApp?.close()
    }

    /**
     * [SentryDsnProvider] is deliberately never bound inside `shared`'s own [appModules] — each
     * platform's app-level bootstrap supplies it (see that interface's own KDoc), the same
     * [com.togetherly.data.purchase.RevenueCatApiKeyProvider]/
     * [com.togetherly.data.telemetry.PostHogApiKeyProvider] shape. This test graph stands in for
     * that bootstrap module with an always-absent DSN, matching production's own "missing
     * configuration falls back to no-op diagnostics" behavior — it exists only so
     * [com.togetherly.core.telemetry.OperationalDiagnostics] (now a dependency reachable from
     * [QuestRepository] via the content-loading capture boundary) has *something* to resolve, not
     * to exercise Sentry itself. `debug = false` here specifically to skip that missing-DSN path's
     * debug-only warning log — [com.togetherly.core.logging.AppLogger]'s real Android
     * implementation calls `android.util.Log`, unmocked (and out of scope to mock) under this
     * plain-JVM `androidHostTest` target.
     */
    private fun productionGraph(): KoinApplication {
        val application = koinApplication {
            modules(
                appModules(
                    AppConfiguration(
                        applicationName = "Togetherly",
                        debug = false,
                    ),
                ) + module {
                    single<SentryDsnProvider> { SentryDsnProvider { null } }
                },
            )
        }
        koinApp = application
        return application
    }

    @Test
    fun resolvesCoreAbstractionsFromTheSharedGraph() {
        val application = productionGraph()

        application.koin.get<AppDispatchers>()
        application.koin.get<AppClock>()
        application.koin.get<IdGenerator>()
    }

    /**
     * Every stage of the content pipeline (Steps 5.1-5.4) must resolve independently from the
     * real production graph, not just the [QuestRepository] it ultimately feeds — a missing
     * binding for any one stage would otherwise only surface once someone actually calls
     * [QuestRepository], not at graph-verification time.
     */
    @Test
    fun resolvesEveryContentPipelineComponentFromTheSharedGraph() {
        val application = productionGraph()

        application.koin.get<QuestCatalogueResource>()
        application.koin.get<QuestCatalogueParser>()
        application.koin.get<QuestCatalogueValidator>()
        application.koin.get<FamilyQuestMapper>()
        application.koin.get<QuestPackMapper>()
        application.koin.get<QuestCatalogueMapper>()
        application.koin.get<QuestCatalogueLoader>()
    }

    @Test
    fun resolvesTheBundledQuestRepositoryFromTheSharedGraph() {
        val application = productionGraph()

        application.koin.get<QuestRepository>()
    }
}
