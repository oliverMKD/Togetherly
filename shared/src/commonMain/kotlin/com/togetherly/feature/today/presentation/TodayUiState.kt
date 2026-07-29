package com.togetherly.feature.today.presentation

import androidx.compose.runtime.Immutable
import com.togetherly.core.ui.UiText
import com.togetherly.feature.today.model.TodayContentState
import com.togetherly.feature.today.model.TodayFilterState

/**
 * Holds nothing platform-shaped (`NavController`, `Context`, `SnackbarHostState`, a domain
 * repository, a mutable collection) — the same rule
 * [com.togetherly.feature.onboarding.model.OnboardingUiState] follows.
 *
 * Reveal is presentation-only, not persisted completion state: [TodayContentState.Mystery] vs
 * [TodayContentState.Revealed] lives only in [content], never written to
 * [com.togetherly.domain.daily.DailyQuest]. A quest starts [TodayContentState.Mystery] every time
 * Today first loads for a process (`ScreenStarted`); [com.togetherly.feature.today.presentation.TodayAction.RevealClicked]
 * flips it to [TodayContentState.Revealed], and that flip survives configuration change (it's
 * ordinary [ViewModel][androidx.lifecycle.ViewModel] state) but not process death — a fresh process
 * re-resolves the same persisted [com.togetherly.domain.daily.DailyQuest] and starts Mystery again.
 * A successful reroll or a successful filter apply both return the *new* quest to
 * [TodayContentState.Mystery] — reveal never carries over to a different quest. None of this
 * counts as completion or starts a quest session; those remain entirely separate, explicit actions
 * (Quest Detail's own Start flow, Step 8.6).
 *
 * [filters] is draft-only — see [TodayFilterState]'s own KDoc. [rerollsRemaining] mirrors
 * [com.togetherly.domain.daily.RerollAllowance.remaining] for the currently resolved quest; `null`
 * means unlimited (Family Plus) or not yet known (still loading).
 *
 * [rerollConfirmationVisible] gates the reroll's own confirmation dialog — a reroll is never
 * requested from the domain layer the instant the user taps "Try another quest"; see
 * [com.togetherly.feature.today.presentation.TodayViewModel.onRerollClicked]'s own KDoc for why
 * that has to be a separate, explicit confirm step.
 */
@Immutable
data class TodayUiState(
    val greeting: UiText,
    val familyName: String?,
    val content: TodayContentState = TodayContentState.Loading,
    val filters: TodayFilterState = TodayFilterState(),
    val filtersVisible: Boolean = false,
    val rerollsRemaining: Int? = null,
    val rerollConfirmationVisible: Boolean = false,
    val isApplyingFilters: Boolean = false,
    val isRerolling: Boolean = false,
    val transientError: UiText? = null,
)
