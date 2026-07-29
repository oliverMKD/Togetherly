package com.togetherly.domain.completion.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FakeCompletionRepositoryContractTest : CompletionRepositoryContractTest() {

    override fun repository(): CompletionRepository = FakeCompletionRepository()
}
