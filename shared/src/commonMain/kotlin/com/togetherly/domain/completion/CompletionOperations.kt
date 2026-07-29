package com.togetherly.domain.completion

import kotlin.time.Instant

fun ActiveQuestSession.complete(
    completedAt: Instant,
    note: MemoryNote? = null,
    reactions: Set<FamilyReaction> = emptySet(),
): QuestCompletion = QuestCompletion(
    id = completionId,
    familyId = familyId,
    questId = questId,
    questVersion = questVersion,
    startedAt = startedAt,
    completedAt = completedAt,
    note = note,
    reactions = reactions,
    media = emptyList(),
)

fun QuestCompletion.attachMedia(
    media: MemoryMedia,
): QuestCompletion = copy(media = this.media + media)

/**
 * Returns this completion unchanged when [mediaId] is not attached — there is no existing
 * "not found" error convention in this domain, so removal of an absent item is a no-op.
 */
fun QuestCompletion.removeMedia(
    mediaId: MemoryMediaId,
): QuestCompletion = copy(media = media.filterNot { it.id == mediaId })
