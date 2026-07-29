package com.togetherly.content.model

import kotlinx.serialization.Serializable

/**
 * Order is an explicit field, never derived from array position, so a validator can detect
 * malformed authoring (gaps, duplicates, wrong starting point) independently of JSON array order.
 */
@Serializable
internal data class InstructionStepDto(
    val order: Int,
    val text: String,
)
