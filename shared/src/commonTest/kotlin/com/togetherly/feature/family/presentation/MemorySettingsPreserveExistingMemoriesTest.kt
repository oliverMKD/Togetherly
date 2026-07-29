package com.togetherly.feature.family.presentation

import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.repository.FakeCompletionRepository
import com.togetherly.domain.completion.validQuestCompletion
import com.togetherly.domain.family.MemoryPreferences
import com.togetherly.domain.family.repository.FakeFamilySettingsRepository
import com.togetherly.domain.family.testFamilySettings
import com.togetherly.domain.family.usecase.UpdateMemoryPreferences
import com.togetherly.integration.testFamilyProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Step 13.5's own requirement: disabling a memory type "does not remove files automatically" and
 * must "preserve existing memories of that type." [MemorySettingsViewModel] has no dependency on
 * [FakeCompletionRepository]/media storage at all — this test proves that structurally, not just
 * by inspection: a completion already saved with photo/voice/note content is byte-for-byte
 * unchanged after every memory type is disabled through the real save path.
 */
class MemorySettingsPreserveExistingMemoriesTest {

    @Test
    fun disablingEveryMemoryTypeNeverTouchesAnAlreadySavedCompletion() = runTest {
        val completionId = CompletionId("completion-1")
        val existingPhoto = MemoryMedia.Photo(MemoryMediaId("media-1"), MediaReference("completions/c/photo-1.jpg"))
        val existingVoice = MemoryMedia.Voice(MemoryMediaId("media-2"), MediaReference("completions/c/voice-1.m4a"), 12.seconds)
        val completionRepository = FakeCompletionRepository()
        completionRepository.saveCompletion(
            validQuestCompletion(
                id = completionId,
                note = MemoryNote("A lovely afternoon."),
                reactions = setOf(FamilyReaction.HAPPY),
                media = listOf(existingPhoto, existingVoice),
            ),
        )
        val before = (completionRepository.getCompletion(completionId) as DataResult.Success).value

        val settingsRepository = FakeFamilySettingsRepository()
        settingsRepository.setSettings(testFamilySettings(profile = testFamilyProfile()))
        val updateMemoryPreferences = UpdateMemoryPreferences(settingsRepository)
        updateMemoryPreferences(
            MemoryPreferences(allowPhotos = false, allowVoiceMemories = false, allowTextNotes = false, showMemoryPromptAfterQuests = false),
        )

        val after = (completionRepository.getCompletion(completionId) as DataResult.Success).value
        assertEquals(before, after)
        assertEquals(listOf(existingPhoto, existingVoice), after?.media)
    }
}
