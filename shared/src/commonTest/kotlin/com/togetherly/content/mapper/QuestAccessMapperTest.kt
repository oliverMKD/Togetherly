package com.togetherly.content.mapper

import com.togetherly.content.model.QuestAccessDto
import com.togetherly.domain.purchase.EntitlementId
import com.togetherly.domain.quest.QuestAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QuestAccessMapperTest {

    @Test
    fun freeAccessMapsToFree() {
        val result = mapQuestAccess(QuestAccessDto(type = "free"), "packs[0].access")

        assertEquals(ContentMappingResult.Success(QuestAccess.Free), result)
    }

    @Test
    fun premiumAccessMapsToPremiumWithDtoEntitlement() {
        val result = mapQuestAccess(
            QuestAccessDto(type = "premium", entitlementId = "family_plus"),
            "packs[0].access",
        )

        assertEquals(ContentMappingResult.Success(QuestAccess.Premium(EntitlementId("family_plus"))), result)
    }

    @Test
    fun unknownAccessTypeFails() {
        val result = mapQuestAccess(QuestAccessDto(type = "vip"), "packs[0].access")

        assertIs<ContentMappingResult.Failure>(result)
        assertEquals(ContentMappingIssueCode.UNKNOWN_ENUM_VALUE, result.issue.code)
        assertEquals("packs[0].access.type", result.issue.path)
    }

    @Test
    fun freeAccessContainingEntitlementFails() {
        val result = mapQuestAccess(
            QuestAccessDto(type = "free", entitlementId = "family_plus"),
            "packs[0].access",
        )

        assertIs<ContentMappingResult.Failure>(result)
        assertEquals(ContentMappingIssueCode.INVALID_ACCESS_COMBINATION, result.issue.code)
    }

    @Test
    fun premiumAccessMissingEntitlementFails() {
        val result = mapQuestAccess(QuestAccessDto(type = "premium"), "packs[0].access")

        assertIs<ContentMappingResult.Failure>(result)
        assertEquals(ContentMappingIssueCode.INVALID_ACCESS_COMBINATION, result.issue.code)
    }
}
