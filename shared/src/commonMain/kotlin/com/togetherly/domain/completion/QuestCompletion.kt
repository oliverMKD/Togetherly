package com.togetherly.domain.completion

import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.validation.DomainValidationException
import com.togetherly.domain.validation.DomainValidationReason
import com.togetherly.domain.validation.requirePositive
import com.togetherly.domain.validation.requireUniqueValues
import kotlin.time.Instant

/**
 * [note] and [media] are private family memory data. Their content must never be included in
 * analytics events, crash reports, or logs. The data layer owns secure local storage of the
 * referenced media; this model only describes valid domain state, not storage or logging behavior.
 */
data class QuestCompletion(
    val id: CompletionId,
    val familyId: FamilyId,
    val questId: QuestId,
    val questVersion: Int,
    val startedAt: Instant?,
    val completedAt: Instant,
    val note: MemoryNote?,
    val reactions: Set<FamilyReaction>,
    val media: List<MemoryMedia>,
) {
    init {
        requirePositive(questVersion)
        if (startedAt != null && completedAt < startedAt) {
            throw DomainValidationException(DomainValidationReason.INVALID_ORDER)
        }
        requireUniqueValues(media.map { it.id })
        requireAtMostOnePhotoAndVoice(media)
    }
}

private fun requireAtMostOnePhotoAndVoice(media: List<MemoryMedia>) {
    val photoCount = media.count { it is MemoryMedia.Photo }
    val voiceCount = media.count { it is MemoryMedia.Voice }
    if (photoCount > 1 || voiceCount > 1) {
        throw DomainValidationException(DomainValidationReason.DUPLICATE_VALUE)
    }
}
