package com.togetherly.domain.daily.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FakeDailyQuestRepositoryContractTest : DailyQuestRepositoryContractTest() {

    override fun repository(): DailyQuestRepository = FakeDailyQuestRepository()
}
