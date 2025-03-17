package com.meninocoiso.beatstarcommunity.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Channel for communication between service and view model
    private val downloadEventChannel = Channel<DownloadEvent>(Channel.BUFFERED)
    private val downloadEventFlow = downloadEventChannel.receiveAsFlow()

    fun startDownload(chartId: String, chartUrl: String, chartName: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra(DownloadService.EXTRA_CHART_ID, chartId)
            putExtra(DownloadService.EXTRA_CHART_URL, chartUrl)
            putExtra(DownloadService.EXTRA_CHART_NAME, chartName)
            putExtra(DownloadService.EXTRA_CHANNEL_NAME, downloadEventChannel.toString())
        }
        context.startForegroundService(intent)
    }

    fun observeDownload(): Flow<DownloadEvent> = downloadEventFlow

    // Method for the service to send events
    suspend fun sendEvent(event: DownloadEvent) {
        downloadEventChannel.send(event)
    }
}