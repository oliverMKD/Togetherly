package com.togetherly.feature.completion.presentation

/**
 * [AddMemoryClicked] is the only forward action from a successfully loaded celebration — it leads
 * into memory capture (Step 10.4), where the actual "save a memory" vs. "skip it" choice lives.
 * [ContinueClicked] is kept separate for the *error* state's "Close" action specifically: bailing
 * out there must never attempt to open memory capture for a completion this screen was never able
 * to confirm actually loaded.
 */
sealed interface CompletionCelebrationAction {
    data object ScreenStarted : CompletionCelebrationAction
    data object RetryClicked : CompletionCelebrationAction
    data object ContinueClicked : CompletionCelebrationAction
    data object AddMemoryClicked : CompletionCelebrationAction
}
