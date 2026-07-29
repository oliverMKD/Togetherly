package com.togetherly.domain.daily.usecase

import com.togetherly.domain.daily.DailyQuest
import com.togetherly.domain.daily.RerollAllowance
import com.togetherly.domain.quest.FamilyQuest

data class ResolvedDailyQuest(
    val dailyQuest: DailyQuest,
    val quest: FamilyQuest,
    val rerollAllowance: RerollAllowance,
)
