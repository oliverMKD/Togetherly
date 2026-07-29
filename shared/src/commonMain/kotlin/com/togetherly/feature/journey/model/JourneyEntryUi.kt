package com.togetherly.feature.journey.model

import androidx.compose.runtime.Immutable
import com.togetherly.domain.completion.CompletionId
import com.togetherly.feature.today.model.QuestCategoryUi
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * One row of the private, chronological memory timeline. Holds no domain or Room model — see
 * [com.togetherly.feature.journey.mapper.toUi]. [questTitle]/[category] are both null exactly when
 * the quest behind [completionId] can no longer be resolved (removed content, historical data);
 * the entry itself always still renders — see [com.togetherly.domain.journey.JourneyEntry]'s own
 * KDoc. [completedDate]/[completedTime] are pre-formatted strings, never a raw [kotlin.time.Instant]
 * or platform date type — formatting happens once, in the mapper, not on every recomposition.
 */
@Immutable
data class JourneyEntryUi(
    val completionId: CompletionId,
    val questTitle: String?,
    val category: QuestCategoryUi?,
    val completedDate: String,
    val completedTime: String,
    val note: String?,
    val reactions: PersistentList<ReactionUi> = persistentListOf(),
    val photo: PrivatePhotoUi? = null,
    val voice: VoiceMemoryUi? = null,
)
