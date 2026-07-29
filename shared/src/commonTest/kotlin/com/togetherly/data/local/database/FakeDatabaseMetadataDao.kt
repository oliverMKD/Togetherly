package com.togetherly.data.local.database

/**
 * In-memory stand-in for Room's generated [DatabaseMetadataDao] — lets
 * [com.togetherly.data.purchase.EntitlementCache] be constructed in `commonTest` without
 * Room/Robolectric. The `throwOnNext*` methods (single-shot, mirroring [FakePostHogSdkAdapter]/
 * [FakeSentrySdkAdapter]'s own `throwOnNext*` pattern) exist for Step 14.6's offline-behavior test
 * suite — simulating a local storage I/O failure, the on-device equivalent of "the network is
 * unavailable" for a component that never talks to the network at all.
 */
internal class FakeDatabaseMetadataDao : DatabaseMetadataDao {

    private val values = mutableMapOf<String, String>()

    private var throwOnGetValue: Throwable? = null
    private var throwOnSet: Throwable? = null
    private var throwOnDelete: Throwable? = null

    fun throwOnNextGetValue(throwable: Throwable) {
        throwOnGetValue = throwable
    }

    fun throwOnNextSet(throwable: Throwable) {
        throwOnSet = throwable
    }

    fun throwOnNextDelete(throwable: Throwable) {
        throwOnDelete = throwable
    }

    override suspend fun getValue(key: String): String? {
        throwOnGetValue?.let {
            throwOnGetValue = null
            throw it
        }
        return values[key]
    }

    override suspend fun set(entity: DatabaseMetadataEntity) {
        throwOnSet?.let {
            throwOnSet = null
            throw it
        }
        values[entity.key] = entity.value
    }

    override suspend fun delete(key: String) {
        throwOnDelete?.let {
            throwOnDelete = null
            throw it
        }
        values.remove(key)
    }
}
