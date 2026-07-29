package com.togetherly.domain.quest

import com.togetherly.domain.purchase.EntitlementId

sealed interface QuestAccess {
    data object Free : QuestAccess

    data class Premium(
        val requiredEntitlement: EntitlementId,
    ) : QuestAccess
}
