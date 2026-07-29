package com.togetherly.feature.questmode.model

/**
 * What Quest Mode actually renders for the timer — never
 * [com.togetherly.domain.questmode.QuestTimerState] itself. [Running.displayTime] is already
 * formatted (see [com.togetherly.feature.questmode.mapper.toTimerDisplay]) — the Composable never
 * formats a [kotlin.time.Duration] itself.
 */
sealed interface QuestTimerUi {

    data object Hidden : QuestTimerUi

    data class Running(
        val displayTime: String,
        val progress: Float,
    ) : QuestTimerUi

    data object Finished : QuestTimerUi
}
