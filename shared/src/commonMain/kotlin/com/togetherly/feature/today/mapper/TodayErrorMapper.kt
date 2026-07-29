package com.togetherly.feature.today.mapper

import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.ui.UiText
import com.togetherly.core.ui.toUiText
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.today_no_match_all_in_cooldown
import togetherly.shared.generated.resources.today_no_match_no_age_compatible
import togetherly.shared.generated.resources.today_no_match_no_context_compatible

/**
 * Today-only: maps the three no-match [ContentError]s ([SelectDailyQuestForContext][com.togetherly.domain.daily.usecase.SelectDailyQuestForContext]/
 * [RerollDailyQuest][com.togetherly.domain.daily.usecase.RerollDailyQuest] failures) to specific,
 * actionable copy — deliberately *not* added to the shared [com.togetherly.core.ui.toUiText]
 * mapper, whose own contract is that every [AppError] resolves to the same generic message (see
 * its KDoc). That's still true for every other feature and every other [AppError] here; only
 * Today's filter/reroll flow has something reason-specific and safe to say, so only Today's own
 * error handling asks for it.
 */
fun AppError.toTodayUiText(): UiText = when (this) {
    AppError.Content(ContentError.NO_AGE_COMPATIBLE_QUEST) -> UiText.Resource(Res.string.today_no_match_no_age_compatible)
    AppError.Content(ContentError.NO_CONTEXT_COMPATIBLE_QUEST) -> UiText.Resource(Res.string.today_no_match_no_context_compatible)
    AppError.Content(ContentError.ALL_CANDIDATES_IN_COOLDOWN) -> UiText.Resource(Res.string.today_no_match_all_in_cooldown)
    else -> toUiText()
}
