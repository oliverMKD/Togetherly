package com.togetherly.content.model

import kotlinx.serialization.Serializable

/**
 * Expected [type] values are "free" and "premium" — enforced later by catalogue validation, not
 * here. A plain explicit DTO is used instead of polymorphic JSON since the two-case shape is
 * simple enough that a discriminator field adds no real value over one.
 */
@Serializable
internal data class QuestAccessDto(
    val type: String,
    val entitlementId: String? = null,
)
