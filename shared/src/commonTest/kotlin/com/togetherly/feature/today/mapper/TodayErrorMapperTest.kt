package com.togetherly.feature.today.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.StorageError
import com.togetherly.core.ui.toUiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TodayErrorMapperTest {

    @Test
    fun noAgeCompatibleQuestGetsItsOwnCopy() {
        val error = AppError.Content(ContentError.NO_AGE_COMPATIBLE_QUEST)

        assertNotEquals(error.toUiText(), error.toTodayUiText())
    }

    @Test
    fun noContextCompatibleQuestGetsItsOwnCopy() {
        val error = AppError.Content(ContentError.NO_CONTEXT_COMPATIBLE_QUEST)

        assertNotEquals(error.toUiText(), error.toTodayUiText())
    }

    @Test
    fun allCandidatesInCooldownGetsItsOwnCopy() {
        val error = AppError.Content(ContentError.ALL_CANDIDATES_IN_COOLDOWN)

        assertNotEquals(error.toUiText(), error.toTodayUiText())
    }

    @Test
    fun everyOtherErrorFallsBackToTheGenericMapper() {
        val error = AppError.Storage(StorageError.WRITE_FAILED)

        assertEquals(error.toUiText(), error.toTodayUiText())
    }

    @Test
    fun theThreeNoMatchCopiesAreAllDistinctFromEachOther() {
        val texts = listOf(
            AppError.Content(ContentError.NO_AGE_COMPATIBLE_QUEST).toTodayUiText(),
            AppError.Content(ContentError.NO_CONTEXT_COMPATIBLE_QUEST).toTodayUiText(),
            AppError.Content(ContentError.ALL_CANDIDATES_IN_COOLDOWN).toTodayUiText(),
        )

        assertEquals(texts.toSet().size, texts.size)
    }
}
