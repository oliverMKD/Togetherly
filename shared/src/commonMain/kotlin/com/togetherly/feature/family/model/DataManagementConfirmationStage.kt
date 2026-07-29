package com.togetherly.feature.family.model

/**
 * Which confirmation dialog (if any) [com.togetherly.feature.family.presentation.DataManagementScreen]
 * currently shows. [DELETE_ALL_STAGE_ONE]/[DELETE_ALL_STAGE_TWO] are the two-stage confirmation
 * this feature's own task spec requires specifically for "Delete all local data" — [DELETE_ALL_STAGE_TWO]
 * is rendered as [com.togetherly.designsystem.component.gate.TogetherlyParentalGateDialog] (the
 * "second destructive confirmation dialog plus parental gate" the spec offers as the accessible
 * alternative to a typed-phrase confirmation), reused rather than reinvented. [CONFIRM_DELETE_MEMORIES]/
 * [CONFIRM_RESET_QUEST_HISTORY] are each a single confirmation dialog — less severe, scoped
 * actions that don't warrant the same two-step flow.
 */
enum class DataManagementConfirmationStage {
    NONE,
    CONFIRM_DELETE_MEMORIES,
    CONFIRM_RESET_QUEST_HISTORY,
    DELETE_ALL_STAGE_ONE,
    DELETE_ALL_STAGE_TWO,
}
