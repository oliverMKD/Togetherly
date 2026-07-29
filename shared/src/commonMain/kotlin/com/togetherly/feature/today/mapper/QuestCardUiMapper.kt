package com.togetherly.feature.today.mapper

import com.togetherly.core.ui.UiText
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.FamilyQuest
import com.togetherly.domain.quest.PreparationLevel
import com.togetherly.domain.quest.QuestCategory
import com.togetherly.domain.quest.QuestLocation
import com.togetherly.domain.recommendation.durationBandFor
import com.togetherly.feature.today.model.QuestCardUi
import com.togetherly.feature.today.model.QuestCategoryUi
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_category_create
import togetherly.shared.generated.resources.today_category_discover
import togetherly.shared.generated.resources.today_category_kindness
import togetherly.shared.generated.resources.today_category_memories
import togetherly.shared.generated.resources.today_category_move
import togetherly.shared.generated.resources.today_category_silly
import togetherly.shared.generated.resources.today_category_talk
import togetherly.shared.generated.resources.today_duration_five_minutes
import togetherly.shared.generated.resources.today_duration_ten_minutes
import togetherly.shared.generated.resources.today_duration_thirty_plus_minutes
import togetherly.shared.generated.resources.today_duration_twenty_minutes
import togetherly.shared.generated.resources.today_energy_active
import togetherly.shared.generated.resources.today_energy_calm
import togetherly.shared.generated.resources.today_energy_moderate
import togetherly.shared.generated.resources.today_location_either
import togetherly.shared.generated.resources.today_location_indoor
import togetherly.shared.generated.resources.today_location_outdoor
import togetherly.shared.generated.resources.today_material_summary
import togetherly.shared.generated.resources.today_material_summary_one
import togetherly.shared.generated.resources.today_preparation_advanced
import togetherly.shared.generated.resources.today_preparation_none
import togetherly.shared.generated.resources.today_preparation_simple_materials

/**
 * Builds the immutable card a Composable actually renders — never hand a
 * [FamilyQuest] to a screen. [FamilyQuest.durationMinutes] is bucketed into the same
 * [DurationBand] the recommendation policy itself uses for duration matching (see
 * [com.togetherly.domain.recommendation.durationBandFor]), so the label a family sees always
 * agrees with the band that was actually matched against.
 */
fun FamilyQuest.toCardUi(isSaved: Boolean): QuestCardUi = QuestCardUi(
    id = id,
    title = title.value,
    summary = summary.value,
    category = category.toUi(),
    durationLabel = durationBandFor(durationMinutes).label(),
    locationLabel = location.label(),
    preparationLabel = preparation.label(),
    energyLabel = energy.label(),
    materialSummary = materials.size.takeIf { it > 0 }?.let { count ->
        if (count == 1) {
            UiText.Resource(Res.string.today_material_summary_one)
        } else {
            UiText.Resource(Res.string.today_material_summary, listOf(count))
        }
    },
    isSaved = isSaved,
)

fun QuestCategory.toUi(): QuestCategoryUi = when (this) {
    QuestCategory.TALK -> QuestCategoryUi.TALK
    QuestCategory.CREATE -> QuestCategoryUi.CREATE
    QuestCategory.MOVE -> QuestCategoryUi.MOVE
    QuestCategory.KINDNESS -> QuestCategoryUi.KINDNESS
    QuestCategory.DISCOVER -> QuestCategoryUi.DISCOVER
    QuestCategory.SILLY -> QuestCategoryUi.SILLY
    QuestCategory.MEMORIES -> QuestCategoryUi.MEMORIES
}

fun QuestCategoryUi.label(): UiText = UiText.Resource(
    when (this) {
        QuestCategoryUi.TALK -> Res.string.today_category_talk
        QuestCategoryUi.CREATE -> Res.string.today_category_create
        QuestCategoryUi.MOVE -> Res.string.today_category_move
        QuestCategoryUi.KINDNESS -> Res.string.today_category_kindness
        QuestCategoryUi.DISCOVER -> Res.string.today_category_discover
        QuestCategoryUi.SILLY -> Res.string.today_category_silly
        QuestCategoryUi.MEMORIES -> Res.string.today_category_memories
    },
)

fun DurationBand.label(): UiText = UiText.Resource(
    when (this) {
        DurationBand.FIVE_MINUTES -> Res.string.today_duration_five_minutes
        DurationBand.TEN_MINUTES -> Res.string.today_duration_ten_minutes
        DurationBand.TWENTY_MINUTES -> Res.string.today_duration_twenty_minutes
        DurationBand.THIRTY_PLUS_MINUTES -> Res.string.today_duration_thirty_plus_minutes
    },
)

fun QuestLocation.label(): UiText = UiText.Resource(
    when (this) {
        QuestLocation.INDOOR -> Res.string.today_location_indoor
        QuestLocation.OUTDOOR -> Res.string.today_location_outdoor
        QuestLocation.EITHER -> Res.string.today_location_either
    },
)

fun PreparationLevel.label(): UiText = UiText.Resource(
    when (this) {
        PreparationLevel.NONE -> Res.string.today_preparation_none
        PreparationLevel.SIMPLE_MATERIALS -> Res.string.today_preparation_simple_materials
        PreparationLevel.ADVANCED -> Res.string.today_preparation_advanced
    },
)

fun EnergyLevel.label(): UiText = UiText.Resource(
    when (this) {
        EnergyLevel.CALM -> Res.string.today_energy_calm
        EnergyLevel.MODERATE -> Res.string.today_energy_moderate
        EnergyLevel.ACTIVE -> Res.string.today_energy_active
    },
)
