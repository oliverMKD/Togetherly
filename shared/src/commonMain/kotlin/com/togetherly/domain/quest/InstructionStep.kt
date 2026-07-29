package com.togetherly.domain.quest

import com.togetherly.domain.validation.requirePositive

data class InstructionStep(
    val order: Int,
    val text: InstructionText,
) {
    init {
        requirePositive(order)
    }
}
