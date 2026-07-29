package com.togetherly.domain.daily

import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

private val SELECTED_AT = Instant.parse("2026-06-15T08:00:00Z")
private val TODAY = LocalDate(2026, 6, 15)
private val EMPTY_CONTEXT = QuestContext(
    duration = null,
    location = null,
    energy = null,
    preparation = null,
    preferredCategory = null,
)

class DailyQuestTest {

    @Test
    fun validAutomaticSelectionAtIndexZero() {
        val dailyQuest = DailyQuest(
            questId = QuestId("quest-1"),
            localDate = TODAY,
            selectionIndex = 0,
            selectedAt = SELECTED_AT,
            source = DailyQuestSource.AUTOMATIC,
            context = EMPTY_CONTEXT,
        )

        assertEquals(0, dailyQuest.selectionIndex)
        assertEquals(DailyQuestSource.AUTOMATIC, dailyQuest.source)
    }

    @Test
    fun validRerollAtALaterIndex() {
        val dailyQuest = DailyQuest(
            questId = QuestId("quest-2"),
            localDate = TODAY,
            selectionIndex = 2,
            selectedAt = SELECTED_AT,
            source = DailyQuestSource.REROLL,
            context = EMPTY_CONTEXT,
        )

        assertEquals(2, dailyQuest.selectionIndex)
        assertEquals(DailyQuestSource.REROLL, dailyQuest.source)
    }

    @Test
    fun negativeSelectionIndexIsRejected() {
        val exception = assertFailsWith<DomainValidationException> {
            DailyQuest(
                questId = QuestId("quest-1"),
                localDate = TODAY,
                selectionIndex = -1,
                selectedAt = SELECTED_AT,
                source = DailyQuestSource.AUTOMATIC,
                context = EMPTY_CONTEXT,
            )
        }
        assertEquals(DomainValidationReason.NON_POSITIVE_VALUE, exception.reason)
    }

    @Test
    fun contextIsPreserved() {
        val context = QuestContext(
            duration = DurationBand.TEN_MINUTES,
            location = QuestLocation.OUTDOOR,
            energy = EnergyLevel.ACTIVE,
            preparation = PreparationLevel.SIMPLE_MATERIALS,
            preferredCategory = QuestCategory.MOVE,
        )

        val dailyQuest = DailyQuest(
            questId = QuestId("quest-1"),
            localDate = TODAY,
            selectionIndex = 0,
            selectedAt = SELECTED_AT,
            source = DailyQuestSource.CONTEXTUAL,
            context = context,
        )

        assertEquals(context, dailyQuest.context)
    }

    @Test
    fun modelExposesOnlyItsDeclaredFieldsWithNoRevealState() {
        val dailyQuest = DailyQuest(
            questId = QuestId("quest-1"),
            localDate = TODAY,
            selectionIndex = 0,
            selectedAt = SELECTED_AT,
            source = DailyQuestSource.AUTOMATIC,
            context = EMPTY_CONTEXT,
        )

        val expectedFields = setOf("questId", "localDate", "selectionIndex", "selectedAt", "source", "context")

        assertEquals(expectedFields, topLevelFieldNames(dailyQuest.toString()))
    }
}

/**
 * Splits a data class's generated toString() into its top-level field names only, ignoring
 * commas inside nested data class values (e.g. `context=QuestContext(location=null, ...)`).
 */
private fun topLevelFieldNames(dataClassToString: String): Set<String> {
    val body = dataClassToString.substringAfter("(").substringBeforeLast(")")
    val fields = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (i in body.indices) {
        when (body[i]) {
            '(' -> depth++
            ')' -> depth--
            ',' -> if (depth == 0) {
                fields += body.substring(start, i)
                start = i + 2
            }
        }
    }
    fields += body.substring(start)
    return fields.map { it.substringBefore("=") }.toSet()
}
