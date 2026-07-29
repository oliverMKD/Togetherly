package com.togetherly.feature.questdetail.mapper

import com.togetherly.core.ui.UiText
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.QuestAccess
import com.togetherly.feature.questdetail.model.QuestDetailUi
import com.togetherly.feature.today.mapper.label
import com.togetherly.feature.today.mapper.toUi
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_age_band_12_to_13
import togetherly.shared.generated.resources.onboarding_age_band_6_to_8
import togetherly.shared.generated.resources.onboarding_age_band_9_to_11

/**
 * Reuses Today's own duration/location/preparation/energy/category label mappers
 * ([com.togetherly.feature.today.mapper]) — the same domain value must read the same way
 * regardless of which screen shows it, so there is exactly one place that mapping lives.
 * [FamilyQuest.instructions] is sorted by [com.togetherly.domain.quest.InstructionStep.order]
 * defensively even though [FamilyQuest]'s own invariant already guarantees a contiguous,
 * already-ordered list for any valid instance.
 *
 * [locked] (Step 12.6) controls whether [QuestDetailUi.instructions]/[QuestDetailUi.hints] are
 * populated at all — when `true` they're always empty lists, never the real content filtered out
 * later by the screen. This is deliberate: a locked-content bug in [com.togetherly.feature.questdetail.presentation.QuestDetailScreen]'s
 * own rendering logic can only ever fail *closed* (nothing to accidentally show) if the data was
 * never mapped in the first place, whereas a "hide it in the UI" approach could leak real
 * instructions through the semantics tree (a screen reader, `printToString()` in a test, etc.) even
 * while visually hidden.
 */
fun FamilyQuest.toDetailUi(locked: Boolean, packTitle: String?): QuestDetailUi = QuestDetailUi(
    id = id,
    title = title.value,
    summary = summary.value,
    category = category.toUi(),
    durationLabel = com.togetherly.domain.recommendation.durationBandFor(durationMinutes).label(),
    locationLabel = location.label(),
    preparationLabel = preparation.label(),
    energyLabel = energy.label(),
    ageBandLabels = ageBands.sortedBy { it.ordinal }.map { it.toUiText() },
    isPremium = access is QuestAccess.Premium,
    packTitle = packTitle,
    instructions = if (locked) emptyList() else instructions.sortedBy { it.order }.map { it.text.value },
    materials = materials.map { it.value },
    hints = if (locked) emptyList() else hints.map { it.value },
    safetyNote = safetyNote?.value,
)

private fun AgeBand.toUiText(): UiText = UiText.Resource(
    when (this) {
        AgeBand.AGE_6_TO_8 -> Res.string.onboarding_age_band_6_to_8
        AgeBand.AGE_9_TO_11 -> Res.string.onboarding_age_band_9_to_11
        AgeBand.AGE_12_TO_13 -> Res.string.onboarding_age_band_12_to_13
    },
)
