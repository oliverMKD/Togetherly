package com.togetherly.feature.packdetails.model

/**
 * The pack's own current access state — always computed from [com.togetherly.domain.purchase.QuestAccessPolicy.canAccess]
 * against a live [com.togetherly.domain.purchase.repository.EntitlementRepository] snapshot, never
 * derived from local UI state alone (this feature's own task spec: "Do not unlock content solely
 * from local UI state"). Drives which primary action [com.togetherly.feature.packdetails.presentation.PackDetailsScreen]
 * shows — [FREE]/[UNLOCKED] both get "Choose a quest", [LOCKED] gets "Unlock with Family Plus".
 */
enum class ContentAccessState {
    FREE,
    LOCKED,
    UNLOCKED,
}
