package com.togetherly.app.di

import com.togetherly.content.loader.DefaultQuestCatalogueLoader
import com.togetherly.content.loader.QuestCatalogueLoader
import com.togetherly.content.mapper.DefaultFamilyQuestMapper
import com.togetherly.content.mapper.DefaultQuestCatalogueMapper
import com.togetherly.content.mapper.DefaultQuestPackMapper
import com.togetherly.content.mapper.FamilyQuestMapper
import com.togetherly.content.mapper.QuestCatalogueMapper
import com.togetherly.content.mapper.QuestPackMapper
import com.togetherly.content.resource.ComposeQuestCatalogueResource
import com.togetherly.content.resource.QuestCatalogueResource
import com.togetherly.content.schema.KotlinxQuestCatalogueParser
import com.togetherly.content.schema.QuestCatalogueParser
import com.togetherly.content.schema.questCatalogueJson
import com.togetherly.content.validation.QuestCatalogueValidator
import org.koin.dsl.module

/**
 * Wires the content *pipeline* only — JSON config, parser, validator, mappers, resource reader,
 * loader. It has no opinion on how a [com.togetherly.domain.quest.repository.QuestRepository] is
 * built from that pipeline; that binding lives in [repositoryModule] so this module stays reusable
 * if the repository wiring ever changes independently of the pipeline itself.
 */
val contentModule = module {
    single { questCatalogueJson }

    single<QuestCatalogueParser> { KotlinxQuestCatalogueParser(json = get()) }

    single { QuestCatalogueValidator() }

    single<FamilyQuestMapper> { DefaultFamilyQuestMapper() }
    single<QuestPackMapper> { DefaultQuestPackMapper() }
    single<QuestCatalogueMapper> { DefaultQuestCatalogueMapper(questMapper = get(), packMapper = get()) }

    single<QuestCatalogueResource> { ComposeQuestCatalogueResource() }

    single<QuestCatalogueLoader> {
        DefaultQuestCatalogueLoader(
            resource = get(),
            parser = get(),
            validator = get(),
            mapper = get(),
            diagnostics = get(),
        )
    }
}
