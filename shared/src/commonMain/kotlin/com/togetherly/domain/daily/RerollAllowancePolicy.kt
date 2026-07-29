package com.togetherly.domain.daily

/**
 * Pure, synchronous — never touches an entitlement repository or the clock itself. Callers
 * resolve [hasFamilyPlus] before calling (see [GetOrSelectDailyQuest][com.togetherly.domain.daily.usecase.GetOrSelectDailyQuest]/
 * [RerollDailyQuest][com.togetherly.domain.daily.usecase.RerollDailyQuest]/
 * [SelectDailyQuestForContext][com.togetherly.domain.daily.usecase.SelectDailyQuestForContext],
 * which resolve it via [com.togetherly.domain.purchase.repository.EntitlementRepository] +
 * [com.togetherly.domain.purchase.QuestAccessPolicy.isFamilyPlusActive]) — this contract itself
 * never needs to change for that.
 */
interface RerollAllowancePolicy {

    /**
     * [selectionIndex] is the current [DailyQuest.selectionIndex] for today: 0 means the
     * automatic first pick with zero rerolls used so far; each increment above 0 is one reroll
     * already used today.
     */
    fun allowance(
        selectionIndex: Int,
        hasFamilyPlus: Boolean,
    ): RerollAllowance
}
