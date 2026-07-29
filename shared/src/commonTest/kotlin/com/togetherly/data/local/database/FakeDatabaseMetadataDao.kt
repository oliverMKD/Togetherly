package com.togetherly.data.local.database

/** In-memory stand-in for Room's generated [DatabaseMetadataDao] — lets [com.togetherly.data.purchase.EntitlementCache] be constructed in `commonTest` without Room/Robolectric. */
internal class FakeDatabaseMetadataDao : DatabaseMetadataDao {

    private val values = mutableMapOf<String, String>()

    override suspend fun getValue(key: String): String? = values[key]

    override suspend fun set(entity: DatabaseMetadataEntity) {
        values[entity.key] = entity.value
    }

    override suspend fun delete(key: String) {
        values.remove(key)
    }
}
