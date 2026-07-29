package com.togetherly.domain.purchase

import kotlin.time.Instant

data class AccessSnapshot(
    val familyAccess: FamilyAccess,
    val activeEntitlements: Set<EntitlementId>,
    val verifiedAt: Instant,
)
