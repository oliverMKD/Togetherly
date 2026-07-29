package com.togetherly.data.local.mapper

import com.togetherly.core.result.DataResult

/**
 * The default shape for a one-entity-to-one-domain-model mapping. [toEntity] cannot actually fail
 * — a domain model is already fully validated, and every storage-key conversion is total — but it
 * still returns [DataResult] for symmetry with [toDomain], which genuinely can fail on corrupted
 * storage. Relations spanning multiple entities (family, completion) use focused mapper classes
 * instead of this interface — see their own files.
 */
internal interface EntityMapper<Entity, Domain> {
    fun toEntity(domain: Domain): DataResult<Entity>
    fun toDomain(entity: Entity): DataResult<Domain>
}
