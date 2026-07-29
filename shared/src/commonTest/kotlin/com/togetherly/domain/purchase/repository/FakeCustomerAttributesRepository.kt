package com.togetherly.domain.purchase.repository

import com.togetherly.domain.family.DurationBand

class FakeCustomerAttributesRepository : CustomerAttributesRepository {

    var markOnboardingCompletedCallCount: Int = 0
        private set
    var markFirstQuestCompletedCallCount: Int = 0
        private set
    val preferredDurationBucketCalls: MutableList<DurationBand> = mutableListOf()

    override suspend fun markOnboardingCompleted() {
        markOnboardingCompletedCallCount++
    }

    override suspend fun markFirstQuestCompleted() {
        markFirstQuestCompletedCallCount++
    }

    override suspend fun setPreferredDurationBucket(bucket: DurationBand) {
        preferredDurationBucketCalls += bucket
    }
}
