package com.togetherly.data.local.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.daily.DailyQuestEntity
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestLocation
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val EMPTY_CONTEXT = QuestContext(null, null, null, null, null)
private val FULL_CONTEXT = QuestContext(
    duration = DurationBand.TEN_MINUTES,
    location = QuestLocation.OUTDOOR,
    energy = EnergyLevel.ACTIVE,
    preparation = PreparationLevel.SIMPLE_MATERIALS,
    preferredCategory = QuestCategory.MOVE,
)

class DailyQuestMapperTest {

    private val mapper = DailyQuestMapper()

    private fun testDailyQuest(context: QuestContext) = DailyQuest(
        questId = QuestId("quest-1"),
        localDate = LocalDate(2026, 7, 24),
        selectionIndex = 0,
        selectedAt = Instant.fromEpochMilliseconds(1_690_000_000_000L),
        source = DailyQuestSource.AUTOMATIC,
        context = context,
    )

    @Test
    fun dailyQuestWithEmptyContextRoundTrips() {
        val domain = testDailyQuest(EMPTY_CONTEXT)

        val entity = (mapper.toEntity(domain) as DataResult.Success).value
        assertEquals(null, entity.contextDuration)
        assertEquals(null, entity.contextLocation)
        assertEquals(null, entity.contextEnergy)
        assertEquals(null, entity.contextPreparation)
        assertEquals(null, entity.contextCategory)

        assertEquals(domain, (mapper.toDomain(entity) as DataResult.Success).value)
    }

    @Test
    fun dailyQuestWithFullContextRoundTrips() {
        val domain = testDailyQuest(FULL_CONTEXT)

        val entity = (mapper.toEntity(domain) as DataResult.Success).value
        assertEquals("ten_minutes", entity.contextDuration)
        assertEquals("outdoor", entity.contextLocation)
        assertEquals("active", entity.contextEnergy)
        assertEquals("simple_materials", entity.contextPreparation)
        assertEquals("move", entity.contextCategory)

        assertEquals(domain, (mapper.toDomain(entity) as DataResult.Success).value)
    }

    @Test
    fun invalidLocalDateIsCorruptedStorage() {
        val entity = DailyQuestEntity(
            localDate = "not-a-date",
            questId = "quest-1",
            selectionIndex = 0,
            selectedAtEpochMillis = 1_000L,
            source = "automatic",
            contextDuration = null,
            contextLocation = null,
            contextEnergy = null,
            contextPreparation = null,
            contextCategory = null,
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(entity))
    }

    @Test
    fun unknownContextEnumKeyIsCorruptedStorage() {
        val entity = DailyQuestEntity(
            localDate = "2026-07-24",
            questId = "quest-1",
            selectionIndex = 0,
            selectedAtEpochMillis = 1_000L,
            source = "automatic",
            contextDuration = "not-a-duration",
            contextLocation = null,
            contextEnergy = null,
            contextPreparation = null,
            contextCategory = null,
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(entity))
    }

    @Test
    fun unknownSourceKeyIsCorruptedStorage() {
        val entity = DailyQuestEntity(
            localDate = "2026-07-24",
            questId = "quest-1",
            selectionIndex = 0,
            selectedAtEpochMillis = 1_000L,
            source = "not-a-source",
            contextDuration = null,
            contextLocation = null,
            contextEnergy = null,
            contextPreparation = null,
            contextCategory = null,
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(entity))
    }
}
