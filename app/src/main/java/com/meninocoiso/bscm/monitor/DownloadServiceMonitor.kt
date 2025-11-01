package com.meninocoiso.bscm.monitor

import DownloadEvent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.meninocoiso.bscm.domain.enums.ErrorType
import com.meninocoiso.bscm.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadServiceMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val _downloadEvents = MutableSharedFlow<DownloadEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Prevent memory issues
    )

    // Track active downloads to prevent duplicates and provide status queries
    private val activeDownloads = mutableSetOf<String>()
    private val activeDownloadsLock = Mutex()

    companion object {
        private const val TAG = "DownloadServiceConnection"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * Starts a download with improved error handling and duplicate prevention
     */
    suspend fun startDownload(
        chartId: String,
        bundleUrl: String,
        chartName: String,
        isUpdate: Boolean = false
    ) {
        // Validate inputs
        require(chartId.isNotBlank()) { "Chart ID cannot be blank" }
        require(bundleUrl.isNotBlank()) { "Bundle URL cannot be blank" }
        require(chartName.isNotBlank()) { "Chart name cannot be blank" }

        // Check if download is already active
        activeDownloadsLock.withLock {
            if (activeDownloads.contains(chartId)) {
                Log.w(TAG, "Download already in progress for chart: $chartId")
                sendEvent(DownloadEvent.Error(
                    chartId,
                    "Download already in progress",
                    ErrorType.DOWNLOAD_ERROR
                ))
                return
            }
            activeDownloads.add(chartId)
        }

        try {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(DownloadService.Companion.EXTRA_CHART_ID, chartId)
                putExtra(DownloadService.Companion.EXTRA_BUNDLE_URL, bundleUrl)
                putExtra(DownloadService.Companion.EXTRA_CHART_NAME, chartName)
                putExtra(DownloadService.Companion.EXTRA_IS_UPDATE, isUpdate)
            }

            // Start service with retry logic
            startServiceWithRetry(intent, chartId)

            Log.d(TAG, "Download service started for chart: $chartId")

        } catch (e: Exception) {
            // Remove from active downloads on failure
            activeDownloadsLock.withLock {
                activeDownloads.remove(chartId)
            }

            Log.e(TAG, "Failed to start download service for chart: $chartId", e)
            sendEvent(DownloadEvent.Error(
                chartId,
                "Failed to start download: ${e.message}",
                ErrorType.UNKNOWN
            ))
            throw e
        }
    }

    /**
     * Starts the foreground service with retry logic
     */
    private suspend fun startServiceWithRetry(intent: Intent, chartId: String) {
        var lastException: Exception? = null

        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                context.startForegroundService(intent)
                return // Success
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Failed to start service (attempt ${attempt + 1}/$MAX_RETRY_ATTEMPTS)", e)

                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1)) // Exponential backoff
                }
            }
        }

        // All attempts failed
        throw lastException ?: Exception("Failed to start download service after $MAX_RETRY_ATTEMPTS attempts")
    }


    /**
     * Checks if a download is currently active for the given chart ID
     */
    suspend fun isDownloadActive(chartId: String): Boolean {
        return activeDownloadsLock.withLock {
            activeDownloads.contains(chartId)
        }
    }

    /**
     * Gets a list of all active download IDs
     */
    suspend fun getActiveDownloads(): Set<String> {
        return activeDownloadsLock.withLock {
            activeDownloads.toSet()
        }
    }

    /**
     * Sends an event to all observers with proper error handling
     */
    fun sendEvent(event: DownloadEvent) {
        try {
            val success = _downloadEvents.tryEmit(event)
            if (!success) {
                Log.w(TAG, "Failed to emit download event: $event (buffer may be full)")
            }

            // Handle completion or error events by removing from active downloads
            when (event) {
                is DownloadEvent.Complete,
                is DownloadEvent.Error -> {
                    // Use a coroutine scope to handle the suspend function
                    CoroutineScope(Dispatchers.IO).launch {
                        activeDownloadsLock.withLock {
                            activeDownloads.remove(event.chartId)
                        }
                    }
                }
                else -> { /* No action needed for other event types */ }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending download event", e)
        }
    }

    /**
     * Public API for observing download events
     * Returns a cold SharedFlow that can be collected safely
     */
    fun observeDownload(): SharedFlow<DownloadEvent> = _downloadEvents.asSharedFlow()
}