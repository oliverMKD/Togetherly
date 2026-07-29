package com.togetherly.content.mapper

import com.togetherly.content.model.QuestAccessDto
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.quest.QuestAccess

/**
 * Uses the DTO's own [QuestAccessDto.entitlementId] verbatim — this mapper never assumes or
 * hardcodes a specific entitlement (e.g. "family_plus") for premium content.
 */
internal fun mapQuestAccess(dto: QuestAccessDto, path: String): ContentMappingResult<QuestAccess> =
    when (dto.type) {
        "free" -> if (dto.entitlementId == null) {
            ContentMappingResult.Success(QuestAccess.Free)
        } else {
            ContentMappingResult.Failure(
                ContentMappingIssue("$path.entitlementId", ContentMappingIssueCode.INVALID_ACCESS_COMBINATION, dto.entitlementId),
            )
        }

        "premium" -> {
            val entitlementId = dto.entitlementId
            if (entitlementId == null) {
                ContentMappingResult.Failure(
                    ContentMappingIssue("$path.entitlementId", ContentMappingIssueCode.INVALID_ACCESS_COMBINATION, null),
                )
            } else {
                mapCatching("$path.entitlementId", entitlementId) {
                    QuestAccess.Premium(requiredEntitlement = EntitlementId(entitlementId))
                }
            }
        }

        else -> ContentMappingResult.Failure(
            ContentMappingIssue("$path.type", ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, dto.type),
        )
    }
