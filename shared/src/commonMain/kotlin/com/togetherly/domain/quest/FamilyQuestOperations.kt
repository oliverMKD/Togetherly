package com.togetherly.domain.quest

import com.togetherly.domain.family.AgeBand

/**
 * A quest is compatible only when it supports every configured child age band —
 * the safest default for a cooperative family experience.
 */
fun FamilyQuest.supports(ageBands: Set<AgeBand>): Boolean =
    this.ageBands.containsAll(ageBands)

fun FamilyQuest.matches(location: QuestLocation): Boolean =
    this.location == QuestLocation.EITHER || this.location == location
