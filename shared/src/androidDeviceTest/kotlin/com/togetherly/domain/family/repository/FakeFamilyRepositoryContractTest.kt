package com.togetherly.domain.family.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FakeFamilyRepositoryContractTest : FamilyRepositoryContractTest() {

    override fun repository(): FamilyRepository = FakeFamilyRepository()
}
