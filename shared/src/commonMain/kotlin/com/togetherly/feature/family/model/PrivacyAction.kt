package com.togetherly.feature.family.model

/** A single action — this screen is purely informational, so [BackClicked] is the only intent it ever needs to carry. */
sealed interface PrivacyAction {
    data object BackClicked : PrivacyAction
}
