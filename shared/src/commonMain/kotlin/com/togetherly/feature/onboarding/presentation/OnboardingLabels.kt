package com.togetherly.feature.onboarding.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.domain.family.AgeBand
import com.togetherly.domain.family.DurationBand
import com.togetherly.domain.family.LocationPreference
import com.togetherly.domain.family.PreparationPreference
import com.togetherly.domain.quest.EnergyLevel
import com.togetherly.domain.quest.QuestCategory
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.onboarding_age_band_12_to_13
import togetherly.shared.generated.resources.onboarding_age_band_6_to_8
import togetherly.shared.generated.resources.onboarding_age_band_9_to_11
import togetherly.shared.generated.resources.onboarding_day_friday
import togetherly.shared.generated.resources.onboarding_day_monday
import togetherly.shared.generated.resources.onboarding_day_saturday
import togetherly.shared.generated.resources.onboarding_day_sunday
import togetherly.shared.generated.resources.onboarding_day_thursday
import togetherly.shared.generated.resources.onboarding_day_tuesday
import togetherly.shared.generated.resources.onboarding_day_wednesday
import togetherly.shared.generated.resources.onboarding_duration_10
import togetherly.shared.generated.resources.onboarding_duration_20
import togetherly.shared.generated.resources.onboarding_duration_30_plus
import togetherly.shared.generated.resources.onboarding_duration_5
import togetherly.shared.generated.resources.energy_level_active
import togetherly.shared.generated.resources.energy_level_calm
import togetherly.shared.generated.resources.energy_level_moderate
import togetherly.shared.generated.resources.onboarding_interest_create_description
import togetherly.shared.generated.resources.onboarding_interest_create_label
import togetherly.shared.generated.resources.onboarding_interest_discover_description
import togetherly.shared.generated.resources.onboarding_interest_discover_label
import togetherly.shared.generated.resources.onboarding_interest_kindness_description
import togetherly.shared.generated.resources.onboarding_interest_kindness_label
import togetherly.shared.generated.resources.onboarding_interest_memories_description
import togetherly.shared.generated.resources.onboarding_interest_memories_label
import togetherly.shared.generated.resources.onboarding_interest_move_description
import togetherly.shared.generated.resources.onboarding_interest_move_label
import togetherly.shared.generated.resources.onboarding_interest_silly_description
import togetherly.shared.generated.resources.onboarding_interest_silly_label
import togetherly.shared.generated.resources.onboarding_interest_talk_description
import togetherly.shared.generated.resources.onboarding_interest_talk_label
import togetherly.shared.generated.resources.onboarding_location_both
import togetherly.shared.generated.resources.onboarding_location_indoor
import togetherly.shared.generated.resources.onboarding_location_outdoor
import togetherly.shared.generated.resources.onboarding_preparation_any
import togetherly.shared.generated.resources.onboarding_preparation_none
import togetherly.shared.generated.resources.onboarding_preparation_simple_materials

/**
 * Every domain enum → presentation label (and, for [QuestCategory], description/color) mapping
 * onboarding needs, in one place — used by both the selection steps and the Review summary so
 * neither can drift from the other. Labels/colors intentionally live here, in the feature, never
 * on the domain enums themselves or in the design system (see [QuestCategory]'s own definition:
 * it is deliberately just seven bare names).
 */
@Composable
fun AgeBand.label(): String = stringResource(
    when (this) {
        AgeBand.AGE_6_TO_8 -> Res.string.onboarding_age_band_6_to_8
        AgeBand.AGE_9_TO_11 -> Res.string.onboarding_age_band_9_to_11
        AgeBand.AGE_12_TO_13 -> Res.string.onboarding_age_band_12_to_13
    },
)

@Composable
fun QuestCategory.label(): String = stringResource(
    when (this) {
        QuestCategory.TALK -> Res.string.onboarding_interest_talk_label
        QuestCategory.CREATE -> Res.string.onboarding_interest_create_label
        QuestCategory.MOVE -> Res.string.onboarding_interest_move_label
        QuestCategory.KINDNESS -> Res.string.onboarding_interest_kindness_label
        QuestCategory.DISCOVER -> Res.string.onboarding_interest_discover_label
        QuestCategory.SILLY -> Res.string.onboarding_interest_silly_label
        QuestCategory.MEMORIES -> Res.string.onboarding_interest_memories_label
    },
)

@Composable
fun QuestCategory.description(): String = stringResource(
    when (this) {
        QuestCategory.TALK -> Res.string.onboarding_interest_talk_description
        QuestCategory.CREATE -> Res.string.onboarding_interest_create_description
        QuestCategory.MOVE -> Res.string.onboarding_interest_move_description
        QuestCategory.KINDNESS -> Res.string.onboarding_interest_kindness_description
        QuestCategory.DISCOVER -> Res.string.onboarding_interest_discover_description
        QuestCategory.SILLY -> Res.string.onboarding_interest_silly_description
        QuestCategory.MEMORIES -> Res.string.onboarding_interest_memories_description
    },
)

@Composable
fun QuestCategory.color(): Color = when (this) {
    QuestCategory.TALK -> MaterialTheme.togetherlyColors.categoryTalk
    QuestCategory.CREATE -> MaterialTheme.togetherlyColors.categoryCreate
    QuestCategory.MOVE -> MaterialTheme.togetherlyColors.categoryMove
    QuestCategory.KINDNESS -> MaterialTheme.togetherlyColors.categoryKindness
    QuestCategory.DISCOVER -> MaterialTheme.togetherlyColors.categoryDiscover
    QuestCategory.SILLY -> MaterialTheme.togetherlyColors.categorySilly
    QuestCategory.MEMORIES -> MaterialTheme.togetherlyColors.categoryMemories
}

@Composable
fun DurationBand.label(): String = stringResource(
    when (this) {
        DurationBand.FIVE_MINUTES -> Res.string.onboarding_duration_5
        DurationBand.TEN_MINUTES -> Res.string.onboarding_duration_10
        DurationBand.TWENTY_MINUTES -> Res.string.onboarding_duration_20
        DurationBand.THIRTY_PLUS_MINUTES -> Res.string.onboarding_duration_30_plus
    },
)

@Composable
fun LocationPreference.label(): String = stringResource(
    when (this) {
        LocationPreference.INDOOR -> Res.string.onboarding_location_indoor
        LocationPreference.OUTDOOR -> Res.string.onboarding_location_outdoor
        LocationPreference.BOTH -> Res.string.onboarding_location_both
    },
)

@Composable
fun PreparationPreference.label(): String = stringResource(
    when (this) {
        PreparationPreference.NONE -> Res.string.onboarding_preparation_none
        PreparationPreference.SIMPLE_MATERIALS -> Res.string.onboarding_preparation_simple_materials
        PreparationPreference.ANY -> Res.string.onboarding_preparation_any
    },
)

/** Reused by [com.togetherly.feature.family.presentation.QuestPreferencesScreen] (Step 13.3) — onboarding itself never collects an energy preference. */
@Composable
fun EnergyLevel.label(): String = stringResource(
    when (this) {
        EnergyLevel.CALM -> Res.string.energy_level_calm
        EnergyLevel.MODERATE -> Res.string.energy_level_moderate
        EnergyLevel.ACTIVE -> Res.string.energy_level_active
    },
)

@Composable
fun DayOfWeek.label(): String = stringResource(
    when (this) {
        DayOfWeek.MONDAY -> Res.string.onboarding_day_monday
        DayOfWeek.TUESDAY -> Res.string.onboarding_day_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.onboarding_day_wednesday
        DayOfWeek.THURSDAY -> Res.string.onboarding_day_thursday
        DayOfWeek.FRIDAY -> Res.string.onboarding_day_friday
        DayOfWeek.SATURDAY -> Res.string.onboarding_day_saturday
        DayOfWeek.SUNDAY -> Res.string.onboarding_day_sunday
    },
)
