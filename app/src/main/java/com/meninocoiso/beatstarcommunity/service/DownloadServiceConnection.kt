package com.meninocoiso.beatstarcommunity.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Use a flow instead of channel for simpler API
    private val _downloadEvents = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 64)

    fun startDownload(chartId: String, chartUrl: String, chartName: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra(DownloadService.EXTRA_CHART_ID, chartId)
            putExtra(DownloadService.EXTRA_CHART_URL, chartUrl)
            putExtra(DownloadService.EXTRA_CHART_NAME, chartName)
        }
        context.startForegroundService(intent)
    }

    fun sendEvent(event: DownloadEvent) {
        _downloadEvents.tryEmit(event)
    }

    // Public API for observation
    fun observeDownload(): Flow<DownloadEvent> = _downloadEvents.asSharedFlow()
}