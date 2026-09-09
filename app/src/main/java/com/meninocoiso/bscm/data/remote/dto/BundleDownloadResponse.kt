package com.meninocoiso.bscm.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BundleDownloadResponse(
    val url: String
)
