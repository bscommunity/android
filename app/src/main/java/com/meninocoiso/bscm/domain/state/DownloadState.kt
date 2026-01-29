package com.meninocoiso.bscm.domain.state

import com.meninocoiso.bscm.domain.enums.ErrorType

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val contentId: String, val progress: Float) : DownloadState()
    data class Extracting(val contentId: String, val progress: Float) : DownloadState()
    data class Error(
        val contentId: String,
        val message: String,
        val type: ErrorType? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : DownloadState()
    data class Installed(val contentId: String) : DownloadState()
}