package com.togetherly.domain.completion

/**
 * [note] is a raw, unvalidated string rather than [MemoryNote] because a blank note is a valid
 * request ("clear the note") that only becomes `null` once normalized, whereas [MemoryNote]
 * itself rejects blank values — the use case normalizes this before persisting.
 *
 * [photo] and [voice] use [MediaEdit] rather than a nullable [PendingMediaReference]/
 * [PendingVoiceReference] so that saving over an existing memory can distinguish "nothing new
 * was staged, keep what's there" ([MediaEdit.Unchanged]) from "delete the existing one"
 * ([MediaEdit.Remove]) — both of which a plain null would otherwise mean at once.
 */
data class SaveCompletionMemoryCommand(
    val completionId: CompletionId,
    val note: String?,
    val reactions: Set<FamilyReaction> = emptySet(),
    val photo: MediaEdit<PendingMediaReference> = MediaEdit.Unchanged,
    val voice: MediaEdit<PendingVoiceReference> = MediaEdit.Unchanged,
)

/**
 * Blank (or null) becomes `null`; non-blank must satisfy [MemoryNote]'s own validation, which
 * throws [com.togetherly.domain.validation.DomainValidationException] for surrounding whitespace
 * or an over-length value.
 */
internal fun normalizeMemoryNote(note: String?): MemoryNote? =
    if (note.isNullOrBlank()) null else MemoryNote(note)
