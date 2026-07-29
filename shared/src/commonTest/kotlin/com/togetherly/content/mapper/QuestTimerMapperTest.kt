package com.togetherly.content.mapper

import com.togetherly.content.model.QuestTimerDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class QuestTimerMapperTest {

    @Test
    fun timerConversionConvertsSecondsToDuration() {
        val result = mapQuestTimer(QuestTimerDto(durationSeconds = 300, keepScreenOn = true), "path")

        val timer = (result as ContentMappingResult.Success).value
        assertEquals(300.seconds, timer?.duration)
        assertEquals(true, timer?.keepScreenOn)
    }

    @Test
    fun optionalTimerMapsNullToNull() {
        val result = mapQuestTimer(null, "path")

        assertEquals(ContentMappingResult.Success(null), result)
    }

    @Test
    fun invalidTimerDurationBecomesMappingIssue() {
        val result = mapQuestTimer(QuestTimerDto(durationSeconds = 0), "quests[0].timer")

        assertIs<ContentMappingResult.Failure>(result)
        assertEquals(ContentMappingIssueCode.DOMAIN_VALIDATION_FAILED, result.issue.code)
        assertEquals("quests[0].timer", result.issue.path)
    }
}
