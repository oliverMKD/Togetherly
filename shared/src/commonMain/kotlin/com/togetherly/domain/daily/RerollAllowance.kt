package com.togetherly.domain.daily

import com.togetherly.domain.validation.requireNonNegative

/**
 * [maximum] `null` represents unlimited rerolls (Family Plus); a finite value is the free-tier
 * cap. [remaining]/[canReroll] are derived so callers never have to re-derive "unlimited" from a
 * sentinel like `Int.MAX_VALUE`.
 */
data class RerollAllowance(
    val used: Int,
    val maximum: Int?,
) {
    init {
        requireNonNegative(used)
        maximum?.let { requireNonNegative(it) }
    }

    val remaining: Int?
        get() = maximum?.let { (it - used).coerceAtLeast(0) }

    val canReroll: Boolean
        get() = maximum == null || used < maximum
}
