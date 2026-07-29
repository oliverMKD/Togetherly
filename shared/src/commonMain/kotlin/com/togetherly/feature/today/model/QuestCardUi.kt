package com.togetherly.feature.today.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.domain.quest.QuestId

/**
 * The full [com.togetherly.domain.quest.FamilyQuest] never reaches a Composable — this is what
 * Today's UI actually renders. [durationLabel]/[locationLabel]/[preparationLabel]/[energyLabel]/
 * [materialSummary] are [UiText], not raw [String]: they're app chrome copy ("5 minutes",
 * "Indoors"), which — like every other piece of Today's UI text — must resolve through Compose
 * resources for localization, the same rule the rest of this codebase's `UiState`s already follow
 * (see [com.togetherly.feature.onboarding.model.OnboardingUiState]'s own `UiText` fields). [title]/
 * [summary] stay plain [String]: they're the quest's own bundled content text, already resolved by
 * the content pipeline, not app UI copy needing a further resource lookup.
 *
 * [category] is [QuestCategoryUi], never [com.togetherly.domain.quest.QuestCategory] — see that
 * type's own KDoc for why. No Compose `Color`/`Painter` appears here; category-to-visual-token
 * mapping is a UI-layer concern (Step 8.4).
 */
@Immutable
data class QuestCardUi(
    val id: QuestId,
    val title: String,
    val summary: String,
    val category: QuestCategoryUi,
    val durationLabel: UiText,
    val locationLabel: UiText,
    val preparationLabel: UiText,
    val energyLabel: UiText,
    val materialSummary: UiText?,
    val isSaved: Boolean,
)
