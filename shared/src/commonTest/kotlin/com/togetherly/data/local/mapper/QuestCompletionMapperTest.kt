package com.togetherly.data.local.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.result.DataResult
import com.togetherly.data.local.completion.CompletionReactionEntity
import com.togetherly.data.local.completion.MemoryMediaEntity
import com.togetherly.data.local.completion.QuestCompletionEntity
import com.togetherly.data.local.completion.QuestCompletionWithDetails
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.quest.QuestId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val STARTED_AT = Instant.fromEpochMilliseconds(1_690_000_000_000L)
private val COMPLETED_AT = Instant.fromEpochMilliseconds(1_690_000_300_000L)

private fun testCompletion(
    note: MemoryNote? = null,
    reactions: Set<FamilyReaction> = emptySet(),
    media: List<MemoryMedia> = emptyList(),
) = QuestCompletion(
    id = CompletionId("completion-1"),
    familyId = FamilyId("family-1"),
    questId = QuestId("quest-1"),
    questVersion = 1,
    startedAt = STARTED_AT,
    completedAt = COMPLETED_AT,
    note = note,
    reactions = reactions,
    media = media,
)

private fun relationOf(components: QuestCompletionComponents) = QuestCompletionWithDetails(
    completion = components.completion,
    reactions = components.reactions,
    media = components.media,
)

class QuestCompletionMapperTest {

    private val mapper = QuestCompletionMapper()

    @Test
    fun completionWithoutOptionalMemoryRoundTrips() {
        val domain = testCompletion()

        val components = mapper.toComponents(domain)
        assertEquals(null, components.completion.note)
        assertEquals(emptyList(), components.reactions)
        assertEquals(emptyList(), components.media)

        assertEquals(domain, (mapper.toDomain(relationOf(components)) as DataResult.Success).value)
    }

    @Test
    fun completionWithNoteReactionsPhotoAndVoiceRoundTrips() {
        val photo = MemoryMedia.Photo(id = MemoryMediaId("media-photo"), localReference = MediaReference("ref-photo"))
        val voice = MemoryMedia.Voice(
            id = MemoryMediaId("media-voice"),
            localReference = MediaReference("ref-voice"),
            duration = 42.seconds,
        )
        val domain = testCompletion(
            note = MemoryNote("What a day!"),
            reactions = setOf(FamilyReaction.HAPPY, FamilyReaction.SILLY),
            media = listOf(photo, voice),
        )

        val components = mapper.toComponents(domain)
        assertEquals("What a day!", components.completion.note)
        assertEquals(setOf("happy", "silly"), components.reactions.map { it.reaction }.toSet())
        // Order is preserved exactly as supplied.
        assertEquals(listOf("media-photo", "media-voice"), components.media.map { it.id })

        assertEquals(domain, (mapper.toDomain(relationOf(components)) as DataResult.Success).value)
    }

    @Test
    fun everyReactionRoundTrips() {
        val domain = testCompletion(reactions = FamilyReaction.entries.toSet())

        val components = mapper.toComponents(domain)
        assertEquals(domain, (mapper.toDomain(relationOf(components)) as DataResult.Success).value)
    }

    @Test
    fun durationPrecisionIsPreservedToTheMillisecond() {
        val voice = MemoryMedia.Voice(
            id = MemoryMediaId("media-voice"),
            localReference = MediaReference("ref-voice"),
            duration = 4_321.milliseconds,
        )
        val domain = testCompletion(media = listOf(voice))

        val components = mapper.toComponents(domain)
        assertEquals(4_321L, components.media.single().durationMillis)

        assertEquals(domain, (mapper.toDomain(relationOf(components)) as DataResult.Success).value)
    }

    @Test
    fun photoWithDurationIsCorruptedStorage() {
        val relation = QuestCompletionWithDetails(
            completion = testCompletionEntity(),
            reactions = emptyList(),
            media = listOf(
                MemoryMediaEntity(
                    id = "media-1",
                    completionId = "completion-1",
                    type = "photo",
                    localReference = "ref-1",
                    durationMillis = 1_000L,
                ),
            ),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun voiceWithoutDurationIsCorruptedStorage() {
        val relation = QuestCompletionWithDetails(
            completion = testCompletionEntity(),
            reactions = emptyList(),
            media = listOf(
                MemoryMediaEntity(
                    id = "media-1",
                    completionId = "completion-1",
                    type = "voice",
                    localReference = "ref-1",
                    durationMillis = null,
                ),
            ),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun unknownMediaTypeIsCorruptedStorage() {
        val relation = QuestCompletionWithDetails(
            completion = testCompletionEntity(),
            reactions = emptyList(),
            media = listOf(
                MemoryMediaEntity(
                    id = "media-1",
                    completionId = "completion-1",
                    type = "not-a-type",
                    localReference = "ref-1",
                    durationMillis = null,
                ),
            ),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun duplicateMediaTypeIsCorruptedStorage() {
        val relation = QuestCompletionWithDetails(
            completion = testCompletionEntity(),
            reactions = emptyList(),
            media = listOf(
                MemoryMediaEntity("media-1", "completion-1", "photo", "ref-1", null),
                MemoryMediaEntity("media-2", "completion-1", "photo", "ref-2", null),
            ),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun unknownReactionIsCorruptedStorage() {
        val relation = QuestCompletionWithDetails(
            completion = testCompletionEntity(),
            reactions = listOf(CompletionReactionEntity("completion-1", "not-a-reaction")),
            media = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    @Test
    fun invalidTimestampRelationshipIsCorruptedStorage() {
        val relation = QuestCompletionWithDetails(
            completion = testCompletionEntity().copy(
                startedAtEpochMillis = COMPLETED_AT.toEpochMilliseconds(),
                completedAtEpochMillis = STARTED_AT.toEpochMilliseconds(),
            ),
            reactions = emptyList(),
            media = emptyList(),
        )

        assertEquals(DataResult.Error(AppError.Storage(StorageError.DATA_CORRUPTED)), mapper.toDomain(relation))
    }

    private fun testCompletionEntity() = QuestCompletionEntity(
        id = "completion-1",
        familyId = "family-1",
        questId = "quest-1",
        questVersion = 1,
        startedAtEpochMillis = STARTED_AT.toEpochMilliseconds(),
        completedAtEpochMillis = COMPLETED_AT.toEpochMilliseconds(),
        note = null,
    )
}
