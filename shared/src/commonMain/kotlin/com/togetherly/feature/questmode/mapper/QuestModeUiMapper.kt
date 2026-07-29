package com.togetherly.feature.questmode.mapper

import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.questmode.QuestTimerState
import com.togetherly.feature.questmode.model.InstructionStepUi
import com.togetherly.feature.questmode.model.QuestModeContentUi
import com.togetherly.feature.questmode.model.QuestTimerUi
import com.togetherly.feature.today.mapper.toUi
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Duration

/**
 * Phone-down is always offered ([QuestModeContentUi.phoneDownSupported] is unconditionally `true`)
 * — an untimed quest still benefits from a distraction-free state (see that field's own KDoc), so
 * there is currently no quest property that would ever turn this off.
 */
fun ActiveQuestSession.toContentUi(quest: FamilyQuest, timerState: QuestTimerState): QuestModeContentUi = QuestModeContentUi(
    completionId = completionId,
    questId = quest.id,
    title = quest.title.value,
    category = quest.category.toUi(),
    instructions = quest.instructions.sortedBy { it.order }
        .map { InstructionStepUi(number = it.order, text = it.text.value) }
        .toPersistentList(),
    hints = quest.hints.map { it.value }.toPersistentList(),
    safetyNote = quest.safetyNote?.value,
    timer = timerState.toUi(),
    phoneDownSupported = true,
    keepScreenOnRequested = quest.timer?.keepScreenOn == true,
)

fun QuestTimerState.toUi(): QuestTimerUi = when (this) {
    QuestTimerState.NotRequired -> QuestTimerUi.Hidden
    is QuestTimerState.Running -> QuestTimerUi.Running(displayTime = remaining.toTimerDisplay(), progress = progress)
    is QuestTimerState.Finished -> QuestTimerUi.Finished
}

/**
 * `9:05` / `0:42` / `0:00` — minutes:seconds, seconds zero-padded to two digits, minutes never
 * padded. Durations at or above one hour switch to `H:MM:SS` (hours never padded, minutes/seconds
 * both zero-padded). No platform-specific date/time formatting API is used — this is plain integer
 * arithmetic, identical on every target.
 */
internal fun Duration.toTimerDisplay(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
