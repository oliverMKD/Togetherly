package com.togetherly.feature.family.model

/**
 * The [com.togetherly.feature.family.model.FamilyProfileUiState] fields a validation error can be
 * keyed to — deliberately only the fields the editor actually collects. There is no participant-
 * count or "energy level" entry: [com.togetherly.domain.family.FamilyProfile] has no backing field
 * for either (see [FamilyProfileUiState]'s own KDoc), so there is nothing to validate.
 */
enum class FamilyProfileField {
    FAMILY_NAME,
    AGE_BANDS,
    DURATIONS,
}
