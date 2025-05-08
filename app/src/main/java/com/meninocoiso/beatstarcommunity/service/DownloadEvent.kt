package com.meninocoiso.beatstarcommunity.service

import com.meninocoiso.beatstarcommunity.domain.enums.ErrorType

sealed class DownloadEvent(val chartId: String) {
    class Progress(chartId: String, val progress: Float) : DownloadEvent(chartId)
    class Extracting(chartId: String, val progress: Float) : DownloadEvent(chartId)
    class Complete(chartId: String) : DownloadEvent(chartId)
    class Error(chartId: String, val message: String, val type: ErrorType? = null) : DownloadEvent(chartId)
}