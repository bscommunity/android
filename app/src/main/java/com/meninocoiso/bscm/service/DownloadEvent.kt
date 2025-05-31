package com.meninocoiso.bscm.service

import com.meninocoiso.bscm.domain.enums.ErrorType

sealed class DownloadEvent(val chartId: String) {
    class Progress(chartId: String, val progress: Float) : DownloadEvent(chartId)
    class Extracting(chartId: String, val progress: Float) : DownloadEvent(chartId)
    class Complete(chartId: String) : DownloadEvent(chartId)
    class Error(chartId: String, val message: String, val type: ErrorType? = null) : DownloadEvent(chartId)
}