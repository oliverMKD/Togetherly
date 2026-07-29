package com.togetherly.feature.journey.mapper

import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.journey.JourneyEntry
import com.togetherly.domain.journey.JourneyStar
import com.togetherly.domain.journey.JourneySummary
import com.togetherly.domain.journey.achievedJourneyMilestones
import com.togetherly.feature.journey.model.JourneyEntryUi
import com.togetherly.feature.journey.model.JourneyStarUi
import com.togetherly.feature.journey.model.JourneySummaryUi
import com.togetherly.feature.journey.model.PrivatePhotoUi
import com.togetherly.feature.journey.model.ReactionUi
import com.togetherly.feature.journey.model.VoiceMemoryUi
import com.togetherly.feature.memory.mapper.emoji
import com.togetherly.feature.memory.mapper.label
import com.togetherly.feature.memory.mapper.toVoiceDurationLabel
import com.togetherly.feature.today.mapper.toUi
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** [com.togetherly.domain.completion.QuestCompletion.reactions] is an unordered [Set] — sorted here so the timeline's reaction chips render in a stable order across recompositions instead of shuffling. */
fun JourneyEntry.toUi(timeZone: TimeZone): JourneyEntryUi {
    val localDateTime = completedAt.toLocalDateTime(timeZone)
    val photo = completion.media.filterIsInstance<MemoryMedia.Photo>().firstOrNull()
    val voice = completion.media.filterIsInstance<MemoryMedia.Voice>().firstOrNull()

    return JourneyEntryUi(
        completionId = completion.id,
        questTitle = quest?.title?.value,
        category = quest?.category?.toUi(),
        completedDate = localDateTime.toJourneyDateDisplay(),
        completedTime = localDateTime.toJourneyTimeDisplay(),
        note = completion.note?.value,
        reactions = completion.reactions
            .sortedBy { it.ordinal }
            .map { reaction -> ReactionUi(label = reaction.label(), emoji = reaction.emoji()) }
            .toPersistentList(),
        photo = photo?.let { PrivatePhotoUi(reference = it.localReference) },
        voice = voice?.let {
            VoiceMemoryUi(mediaId = it.id, reference = it.localReference, durationLabel = it.duration.toVoiceDurationLabel())
        },
    )
}

fun JourneyStar.toUi(): JourneyStarUi = JourneyStarUi(
    completionId = completionId,
    position = position,
    visualVariant = visualVariant,
    category = category?.toUi(),
    hasNote = hasNote,
    hasPhoto = hasPhoto,
    hasVoice = hasVoice,
)

fun JourneySummary.toUi(): JourneySummaryUi = JourneySummaryUi(
    totalCompletions = totalCompletions,
    achievedMilestones = achievedJourneyMilestones(totalCompletions).toPersistentSet(),
)
