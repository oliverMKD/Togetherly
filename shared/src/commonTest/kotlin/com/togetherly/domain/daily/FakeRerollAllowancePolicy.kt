package com.togetherly.domain.daily

class FakeRerollAllowancePolicy(
    var result: RerollAllowance = RerollAllowance(used = 0, maximum = 1),
) : RerollAllowancePolicy {

    val calls: MutableList<Pair<Int, Boolean>> = mutableListOf()

    override fun allowance(selectionIndex: Int, hasFamilyPlus: Boolean): RerollAllowance {
        calls += selectionIndex to hasFamilyPlus
        return result
    }
}
