package com.togetherly.core.result

import com.togetherly.core.error.AppError
import com.togetherly.core.error.StorageError
import com.togetherly.core.error.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DataResultTest {

    @Test
    fun successRetainsItsValue() {
        val result = DataResult.Success(42)

        assertEquals(42, result.value)
    }

    @Test
    fun errorRetainsItsTypedError() {
        val error = AppError.Validation(ValidationError.INVALID_INPUT)

        val result = DataResult.Error(error)

        assertEquals(error, result.error)
    }

    @Test
    fun mapTransformsSuccess() {
        val result: DataResult<Int> = DataResult.Success(2)

        val mapped = result.map { it * 10 }

        assertEquals(DataResult.Success(20), mapped)
    }

    @Test
    fun mapPreservesError() {
        val error = AppError.Storage(StorageError.READ_FAILED)
        val result: DataResult<Int> = DataResult.Error(error)

        val mapped = result.map { it * 10 }

        assertEquals(DataResult.Error(error), mapped)
    }

    @Test
    fun flatMapChainsSuccess() {
        val result: DataResult<Int> = DataResult.Success(2)

        val chained = result.flatMap { DataResult.Success(it * 10) }

        assertEquals(DataResult.Success(20), chained)
    }

    @Test
    fun flatMapShortCircuitsError() {
        val error = AppError.Storage(StorageError.READ_FAILED)
        val result: DataResult<Int> = DataResult.Error(error)

        val chained = result.flatMap { DataResult.Success(it * 10) }

        assertEquals(DataResult.Error(error), chained)
    }

    @Test
    fun onSuccessOnlyRunsForSuccess() {
        var invokedWith: Int? = null
        val result: DataResult<Int> = DataResult.Success(5)

        result.onSuccess { invokedWith = it }

        assertEquals(5, invokedWith)
    }

    @Test
    fun onSuccessDoesNotRunForError() {
        var invoked = false
        val result: DataResult<Int> = DataResult.Error(AppError.Storage(StorageError.READ_FAILED))

        result.onSuccess { invoked = true }

        assertFalse(invoked)
    }

    @Test
    fun onErrorOnlyRunsForError() {
        var invokedWith: AppError? = null
        val error = AppError.Storage(StorageError.READ_FAILED)
        val result: DataResult<Int> = DataResult.Error(error)

        result.onError { invokedWith = it }

        assertEquals(error, invokedWith)
    }

    @Test
    fun onErrorDoesNotRunForSuccess() {
        var invoked = false
        val result: DataResult<Int> = DataResult.Success(5)

        result.onError { invoked = true }

        assertFalse(invoked)
    }
}
