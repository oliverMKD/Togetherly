package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult
import com.togetherly.core.result.getOrElse
import com.togetherly.data.local.completion.CompletionReactionEntity
import com.togetherly.data.local.completion.MemoryMediaEntity
import com.togetherly.data.local.completion.QuestCompletionEntity
import com.togetherly.data.local.completion.QuestCompletionWithDetails
import com.togetherly.data.local.keys.MEMORY_MEDIA_TYPE_PHOTO
import com.togetherly.data.local.keys.MEMORY_MEDIA_TYPE_VOICE
import com.togetherly.data.local.keys.toFamilyReaction
import com.togetherly.data.local.keys.toStorageKey
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.quest.QuestId
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

internal data class QuestCompletionComponents(
    val completion: QuestCompletionEntity,
    val reactions: List<CompletionReactionEntity>,
    val media: List<MemoryMediaEntity>,
)

/**
 * Not an [EntityMapper]: a completion spans three tables, so this exposes a focused pair of
 * functions instead — see [EntityMapper]'s own KDoc for why [toComponents] cannot fail.
 *
 * Never logs [QuestCompletionEntity.note] or [MemoryMediaEntity.localReference] — both are
 * private family memory data (see [com.togetherly.domain.completion.QuestCompletion]'s and
 * [com.togetherly.domain.completion.MediaReference]'s own KDocs).
 *
 * Media row order is preserved exactly as returned from storage — this mapper never re-sorts —
 * so mapping the same stored rows always produces the same domain list order. A stored photo row
 * with a non-null duration, a voice row with no duration, an unrecognized `type`, or more than
 * one photo/voice for the same completion (the last enforced by
 * [com.togetherly.domain.completion.QuestCompletion]'s own invariant) are all corrupted storage.
 */
internal class QuestCompletionMapper {

    fun toComponents(domain: QuestCompletion): QuestCompletionComponents {
        val completionId = domain.id.value
        return QuestCompletionComponents(
            completion = QuestCompletionEntity(
                id = completionId,
                familyId = domain.familyId.value,
                questId = domain.questId.value,
                questVersion = domain.questVersion,
                startedAtEpochMillis = domain.startedAt?.toEpochMilliseconds(),
                completedAtEpochMillis = domain.completedAt.toEpochMilliseconds(),
                note = domain.note?.value,
            ),
            reactions = domain.reactions.map { CompletionReactionEntity(completionId, it.toStorageKey()) },
            media = domain.media.map { it.toMediaEntity(completionId) },
        )
    }

    fun toDomain(relation: QuestCompletionWithDetails): DataResult<QuestCompletion> {
        val reactions = mutableSetOf<FamilyReaction>()
        for (row in relation.reactions) {
            reactions += row.reaction.toFamilyReaction().getOrElse { return DataResult.Error(it) }
        }

        val media = mutableListOf<MemoryMedia>()
        for (row in relation.media) {
            media += row.toDomainMedia().getOrElse { return DataResult.Error(it) }
        }

        return mapStorageCatching {
            QuestCompletion(
                id = CompletionId(relation.completion.id),
                familyId = FamilyId(relation.completion.familyId),
                questId = QuestId(relation.completion.questId),
                questVersion = relation.completion.questVersion,
                startedAt = relation.completion.startedAtEpochMillis?.let { Instant.fromEpochMilliseconds(it) },
                completedAt = Instant.fromEpochMilliseconds(relation.completion.completedAtEpochMillis),
                note = relation.completion.note?.let { MemoryNote(it) },
                reactions = reactions,
                media = media,
            )
        }
    }
}

private fun MemoryMedia.toMediaEntity(completionId: String): MemoryMediaEntity = when (this) {
    is MemoryMedia.Photo -> MemoryMediaEntity(
        id = id.value,
        completionId = completionId,
        type = MEMORY_MEDIA_TYPE_PHOTO,
        localReference = localReference.value,
        durationMillis = null,
    )

    is MemoryMedia.Voice -> MemoryMediaEntity(
        id = id.value,
        completionId = completionId,
        type = MEMORY_MEDIA_TYPE_VOICE,
        localReference = localReference.value,
        durationMillis = duration.inWholeMilliseconds,
    )
}

private fun MemoryMediaEntity.toDomainMedia(): DataResult<MemoryMedia> = when (type) {
    MEMORY_MEDIA_TYPE_PHOTO -> if (durationMillis != null) {
        corruptedStorage()
    } else {
        mapStorageCatching { MemoryMedia.Photo(id = MemoryMediaId(id), localReference = MediaReference(localReference)) }
    }

    MEMORY_MEDIA_TYPE_VOICE -> {
        val storedDuration = durationMillis
        if (storedDuration == null) {
            corruptedStorage()
        } else {
            mapStorageCatching {
                MemoryMedia.Voice(
                    id = MemoryMediaId(id),
                    localReference = MediaReference(localReference),
                    duration = storedDuration.milliseconds,
                )
            }
        }
    }

    else -> corruptedStorage()
}
