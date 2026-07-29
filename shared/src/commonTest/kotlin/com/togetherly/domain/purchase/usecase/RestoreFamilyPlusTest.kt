package com.togetherly.domain.purchase.usecase

import com.togetherly.domain.purchase.AccessSnapshot
import com.togetherly.domain.purchase.FamilyAccess
import com.togetherly.domain.purchase.RestoreResult
import com.togetherly.domain.purchase.repository.FakeEntitlementRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val VERIFIED_AT = Instant.parse("2026-06-15T08:00:00Z")
private val FREE_SNAPSHOT = AccessSnapshot(
    familyAccess = FamilyAccess.free(),
    activeEntitlements = emptySet(),
    verifiedAt = VERIFIED_AT,
)

class RestoreFamilyPlusTest {

    @Test
    fun restoreWithoutEntitlementRemainsSuccessfulReconciliation() = runTest {
        val repository = FakeEntitlementRepository(initialAccess = FREE_SNAPSHOT)
        repository.setRestoreResult(RestoreResult.Success(FREE_SNAPSHOT))
        val useCase = RestoreFamilyPlus(repository)

        val result = useCase()

        assertEquals(RestoreResult.Success(FREE_SNAPSHOT), result)
    }
}
