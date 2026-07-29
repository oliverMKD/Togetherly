package com.togetherly.app.foundation

import kotlinx.serialization.Serializable

@Serializable
internal data class AppInstallationInfo(
    val installationId: String,
    val createdAtEpochMilliseconds: Long,
)
