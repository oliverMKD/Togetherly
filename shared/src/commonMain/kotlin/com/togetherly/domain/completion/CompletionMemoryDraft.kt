package com.togetherly.domain.completion

/**
 * Presentation-independent, in-progress memory edit for [completionId] — constructing one never
 * touches storage or creates permanent records; it only becomes durable once passed through
 * `SaveCompletionMemory`. [note] is a raw string (blank is allowed while editing) and
 * [pendingPhoto]/[pendingVoice] describe at most one newly staged file each, since
 * [PendingMediaReference] and [PendingVoiceReference] are single, not collection, fields.
 */
data class CompletionMemoryDraft(
    val completionId: CompletionId,
    val note: String,
    val reactions: Set<FamilyReaction>,
    val pendingPhoto: PendingMediaReference?,
    val pendingVoice: PendingVoiceReference?,
)

/**
 * A draft only ever describes newly staged pending media, never "the existing committed photo/
 * voice should be removed" — that distinction ([MediaEdit.Remove]) has no representation in the
 * draft yet because no editor for a completion's already-committed memory exists as of this step.
 * A `null` pending field therefore always maps to [MediaEdit.Unchanged], not [MediaEdit.Remove].
 */
fun CompletionMemoryDraft.toSaveCommand(): SaveCompletionMemoryCommand = SaveCompletionMemoryCommand(
    completionId = completionId,
    note = note,
    reactions = reactions,
    photo = pendingPhoto?.let { MediaEdit.Replace(it) } ?: MediaEdit.Unchanged,
    voice = pendingVoice?.let { MediaEdit.Replace(it) } ?: MediaEdit.Unchanged,
)
