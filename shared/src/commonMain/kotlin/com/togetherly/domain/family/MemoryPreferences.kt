package com.togetherly.domain.family

/**
 * Whether a family wants photo capture, voice capture, a text note, and the post-quest "add a
 * memory" prompt itself offered during quest completion (Step 10 capture flow, Step 13.5
 * enforcement). Every field defaults to `true` — matching the app's existing behavior before this
 * setting existed (all four were already unconditionally available/shown), so an upgrading install
 * never silently loses a capability it already had (see [defaults]'s own KDoc).
 *
 * [allowTextNotes] replaces the earlier, never-enforced `defaultSaveNote` field (Step 13.1) — that
 * field had zero readers anywhere in the app (its own "default-expanded note" meaning was never
 * wired to any UI), so repurposing its storage column for a setting that actually has an effect,
 * rather than adding a fifth column next to a dead one, was the honest choice. See
 * `MIGRATION_3_4`'s own KDoc for the schema change this required.
 *
 * Disabling any of these only ever changes what's offered in a *future* completion — it never
 * touches an already-saved [com.togetherly.domain.completion.QuestCompletion]'s existing
 * [com.togetherly.domain.completion.MemoryMedia]/note/reactions. See
 * [com.togetherly.feature.memory.presentation.CompletionMemoryViewModel] and
 * [com.togetherly.feature.completion.presentation.CompletionCelebrationViewModel] for where each
 * field is actually enforced.
 */
data class MemoryPreferences(
    val allowPhotos: Boolean,
    val allowVoiceMemories: Boolean,
    val allowTextNotes: Boolean,
    val showMemoryPromptAfterQuests: Boolean,
) {
    companion object {
        /** Safe defaults for a brand-new profile and for any existing profile migrated without this setting yet stored — every capability stays exactly as available as it already was. */
        fun defaults(): MemoryPreferences = MemoryPreferences(
            allowPhotos = true,
            allowVoiceMemories = true,
            allowTextNotes = true,
            showMemoryPromptAfterQuests = true,
        )
    }
}
