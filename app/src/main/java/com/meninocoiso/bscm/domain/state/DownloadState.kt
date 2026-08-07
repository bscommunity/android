package com.meninocoiso.bscm.domain.state

import com.meninocoiso.bscm.domain.enums.ErrorType

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val id: String,
        val progress: Float,
        val installedCount: Int = 0,
        val totalCount: Int = 0
    ) : DownloadState()
    data class Extracting(
        val id: String,
        val progress: Float,
        val installedCount: Int = 0,
        val totalCount: Int = 0
    ) : DownloadState()
    data class Error(
        val id: String,
        val message: String,
        val type: ErrorType? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : DownloadState()
    data class Installed(val id: String) : DownloadState()
}