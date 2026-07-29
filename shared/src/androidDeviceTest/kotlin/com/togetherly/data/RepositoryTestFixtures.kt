package com.togetherly.data

import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.FamilyReaction
import com.togetherly.domain.completion.MediaReference
import com.togetherly.domain.completion.MemoryMedia
import com.togetherly.domain.completion.MemoryMediaId
import com.togetherly.domain.completion.CompletionPrompt
import com.togetherly.domain.completion.MemoryNote
import com.togetherly.domain.completion.QuestCompletion
import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.DailyQuestSource
import com.togetherly.domain.daily.DismissedQuest
import com.togetherly.domain.daily.QuestContext
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.FamilyDisplayName
import com.togetherly.domain.family.FamilyId
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.family.ReminderPreference
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.InstructionStep
import com.togetherly.domain.quest.InstructionText
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.domain.quest.QuestSummary
import com.togetherly.domain.quest.QuestTitle
import com.togetherly.domain.saved.SavedQuest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal val EMPTY_QUEST_CONTEXT = QuestContext(null, null, null, null, null)

internal fun testFamilyProfile(
    id: FamilyId = FamilyId("family-1"),
    displayName: FamilyDisplayName? = FamilyDisplayName("The Smiths"),
    reminderPreference: ReminderPreference? = ReminderPreference(setOf(DayOfWeek.MONDAY), LocalTime(9, 0)),
    createdAt: Instant = Instant.fromEpochMilliseconds(1_690_000_000_000L),
    updatedAt: Instant = Instant.fromEpochMilliseconds(1_690_000_000_000L),
) = FamilyProfile(
    id = id,
    displayName = displayName,
    childAgeBands = setOf(AgeBand.AGE_6_TO_8, AgeBand.AGE_9_TO_11),
    interests = setOf(QuestCategory.TALK, QuestCategory.MOVE),
    preferredDurations = setOf(DurationBand.TEN_MINUTES),
    locationPreference = LocationPreference.BOTH,
    preparationPreference = PreparationPreference.ANY,
    reminderPreference = reminderPreference,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun testDailyQuest(
    questId: QuestId = QuestId("quest-1"),
    localDate: LocalDate = LocalDate(2026, 7, 24),
    selectionIndex: Int = 0,
    source: DailyQuestSource = DailyQuestSource.AUTOMATIC,
) = DailyQuest(
    questId = questId,
    localDate = localDate,
    selectionIndex = selectionIndex,
    selectedAt = Instant.fromEpochMilliseconds(1_690_000_000_000L),
    source = source,
    context = EMPTY_QUEST_CONTEXT,
)

internal fun testDismissedQuest(
    questId: QuestId = QuestId("quest-1"),
    dismissedAt: Instant = Instant.fromEpochMilliseconds(1_690_000_000_000L),
    localDate: LocalDate = LocalDate(2026, 7, 24),
) = DismissedQuest(questId = questId, dismissedAt = dismissedAt, localDate = localDate)

internal fun testSavedQuest(
    questId: QuestId = QuestId("quest-1"),
    savedAt: Instant = Instant.fromEpochMilliseconds(1_690_000_000_000L),
) = SavedQuest(questId = questId, savedAt = savedAt)

internal fun testActiveQuestSession(
    completionId: CompletionId = CompletionId("completion-1"),
    familyId: FamilyId = FamilyId("family-1"),
    questId: QuestId = QuestId("quest-1"),
    questVersion: Int = 1,
    startedAt: Instant = Instant.fromEpochMilliseconds(1_000L),
) = ActiveQuestSession(
    completionId = completionId,
    familyId = familyId,
    questId = questId,
    questVersion = questVersion,
    startedAt = startedAt,
)

internal fun testQuestCompletion(
    id: CompletionId = CompletionId("completion-1"),
    familyId: FamilyId = FamilyId("family-1"),
    questId: QuestId = QuestId("quest-1"),
    questVersion: Int = 1,
    startedAt: Instant? = Instant.fromEpochMilliseconds(1_000L),
    completedAt: Instant = Instant.fromEpochMilliseconds(2_000L),
    note: MemoryNote? = null,
    reactions: Set<FamilyReaction> = emptySet(),
    media: List<MemoryMedia> = emptyList(),
) = QuestCompletion(
    id = id,
    familyId = familyId,
    questId = questId,
    questVersion = questVersion,
    startedAt = startedAt,
    completedAt = completedAt,
    note = note,
    reactions = reactions,
    media = media,
)

internal fun testPhotoMedia(
    id: MemoryMediaId = MemoryMediaId("media-photo"),
    localReference: MediaReference = MediaReference("ref-photo"),
) = MemoryMedia.Photo(id = id, localReference = localReference)

internal fun testVoiceMedia(
    id: MemoryMediaId = MemoryMediaId("media-voice"),
    localReference: MediaReference = MediaReference("ref-voice"),
) = MemoryMedia.Voice(id = id, localReference = localReference, duration = 30.seconds)

internal fun testFamilyQuest(
    id: QuestId = QuestId("quest-1"),
    version: Int = 1,
    category: QuestCategory = QuestCategory.DISCOVER,
) = FamilyQuest(
    id = id,
    version = version,
    title = QuestTitle("Backyard Scavenger Hunt"),
    summary = QuestSummary("Find five hidden treasures together."),
    instructions = listOf(
        InstructionStep(1, InstructionText("Hide five small objects in the yard.")),
        InstructionStep(2, InstructionText("Give clues to find each one.")),
    ),
    category = category,
    ageBands = setOf(AgeBand.AGE_6_TO_8),
    durationMinutes = 20,
    location = QuestLocation.OUTDOOR,
    preparation = PreparationLevel.SIMPLE_MATERIALS,
    energy = EnergyLevel.MODERATE,
    materials = emptyList(),
    hints = emptyList(),
    completionPrompt = CompletionPrompt("Share what you found!"),
    safetyNote = null,
    packId = QuestPackId("pack-1"),
    access = QuestAccess.Free,
    timer = null,
    cooldownDays = 0,
)
