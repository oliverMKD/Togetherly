package com.togetherly.domain.daily

/**
 * One free reroll per local calendar day; Family Plus ([hasFamilyPlus] `true`) is unlimited.
 * [hasFamilyPlus] is resolved by the calling use case (Step 11.6) from
 * [com.togetherly.domain.purchase.repository.EntitlementRepository] — this class stays a pure
 * function of its two parameters and never touches an entitlement repository or the clock itself.
 */
class DefaultRerollAllowancePolicy : RerollAllowancePolicy {

    override fun allowance(
        selectionIndex: Int,
        hasFamilyPlus: Boolean,
    ): RerollAllowance {
        val maximum = if (hasFamilyPlus) null else FREE_TIER_MAXIMUM_REROLLS
        val used = selectionIndex.coerceAtLeast(0)
        return RerollAllowance(used = used, maximum = maximum)
    }

    private companion object {
        const val FREE_TIER_MAXIMUM_REROLLS = 1
    }
}
