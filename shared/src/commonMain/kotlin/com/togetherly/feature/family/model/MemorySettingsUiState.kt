package com.togetherly.feature.family.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText

/**
 * Four booleans, no validation errors — unlike [QuestPreferencesUiState]/[ReminderUiState] there is
 * no "must be non-empty" combination here to reject; every one of the 16 possible on/off
 * combinations is valid. Disabling any of these only ever changes what a *future* quest completion
 * offers — see [com.togetherly.domain.family.MemoryPreferences]'s own KDoc; existing memories are
 * never touched from this screen, which is exactly why [MemorySettingsAction.ManageMemoriesClicked]
 * exists as a separate, explicit path rather than this screen also trying to delete things.
 */
@Immutable
data class MemorySettingsUiState(
    val isLoading: Boolean = true,
    val allowPhotos: Boolean = true,
    val allowVoiceMemories: Boolean = true,
    val allowTextNotes: Boolean = true,
    val showMemoryPromptAfterQuests: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val error: UiText? = null,
)
