package com.togetherly.content.model

import kotlinx.serialization.Serializable

/**
 * [version] is content-authoring metadata only — the domain [com.togetherly.domain.quest.QuestPack]
 * has no version field, and this DTO does not add one to the domain model to match. There is no
 * current mapping consumer for it yet; a future mapping step decides whether it's used at all.
 */
@Serializable
internal data class QuestPackDto(
    val id: String,
    val version: Int,
    val title: String,
    val description: String,
    val category: String? = null,
    val access: QuestAccessDto,
    val questIds: List<String>,
    val artworkKey: String,
    val sortOrder: Int,
)
