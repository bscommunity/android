package com.meninocoiso.bscm.service

import DownloadEvent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.DownloadException
import com.meninocoiso.bscm.data.manager.ExtractionException
import com.meninocoiso.bscm.data.repository.DownloadRepository
import com.meninocoiso.bscm.domain.enums.ErrorType
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.util.StringUtils.getFinalMessage
import com.meninocoiso.bscm.util.StringUtils.getInitialMessage
import com.meninocoiso.bscm.util.StringUtils.getProgressMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import javax.inject.Inject

private const val TAG = "DownloadService"

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var chartManager: ChartManager

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var downloadServiceConnection: DownloadServiceConnection

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val channelId = "download_channel"

    // Track active downloads to prevent duplicates
    private val activeDownloads = mutableSetOf<String>()
    private val activeDownloadsLock = Mutex()
    
    // Track notification IDs for each chart download
    private val chartNotificationIds = mutableMapOf<String, Int>()
    private val notificationIdsLock = Mutex()
    private var nextNotificationId = 1001

    companion object {
        const val EXTRA_CHART_ID = "extra_chart_id"
        const val EXTRA_BUNDLE_URL = "extra_bundle_url"
        const val EXTRA_CHART_NAME = "extra_chart_name"
        const val EXTRA_IS_UPDATE = "extra_is_update"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "DownloadService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Validate intent and extract parameters
        val downloadParams = extractDownloadParams(intent)
        if (downloadParams == null) {
            Log.e(TAG, "Invalid download parameters, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        val (chartId, bundleUrl, chartName, operation) = downloadParams

        // Check for duplicate downloads
        serviceScope.launch {
            activeDownloadsLock.withLock {
                if (activeDownloads.contains(chartId)) {
                    Log.w(TAG, "Download already in progress for chart: $chartId")
                    stopSelf()
                    return@launch
                }
                activeDownloads.add(chartId)
            }

            try {
                performDownload(chartId, bundleUrl, chartName, operation)
            } finally {
                // Always remove from active downloads
                activeDownloadsLock.withLock {
                    activeDownloads.remove(chartId)
                }

                // Stop service if no more active downloads
                activeDownloadsLock.withLock {
                    if (activeDownloads.isEmpty()) {
                        stopSelf()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun extractDownloadParams(intent: Intent?): DownloadParams? {
        if (intent == null) {
            Log.e(TAG, "Received null intent")
            return null
        }

        val chartId = intent.getStringExtra(EXTRA_CHART_ID)
        val bundleUrl = intent.getStringExtra(EXTRA_BUNDLE_URL)
        val chartName = intent.getStringExtra(EXTRA_CHART_NAME)
        val isUpdate = intent.getBooleanExtra(EXTRA_IS_UPDATE, false)

        if (chartId.isNullOrBlank() || bundleUrl.isNullOrBlank() || chartName.isNullOrBlank()) {
            Log.e(TAG, "Missing required parameters - chartId: $chartId, bundleUrl: $bundleUrl, chartName: $chartName")
            return null
        }

        val operation = if (isUpdate) OperationType.UPDATE else OperationType.INSTALL
        return DownloadParams(chartId, bundleUrl, chartName, operation)
    }

    private suspend fun performDownload(
        chartId: String,
        bundleUrl: String,
        chartName: String,
        operation: OperationType
    ) {
        val initialMessage = getInitialMessage(chartName, operation)
        val finalMessage = getFinalMessage(chartName, operation)

        try {
            Log.d(TAG, "Starting download for chart: $chartId, operation: $operation")

            // Get notification ID for this download
            val notificationId = getNotificationId(chartId)

            // Start as foreground service with initial notification
            val initialNotification = createNotification(
                title = initialMessage.title,
                message = initialMessage.message,
                progress = 0
            )

            startForeground(notificationId, initialNotification)

            // Send initial event
            downloadServiceConnection.sendEvent(DownloadEvent.Started(chartId))

            // Perform the download with comprehensive error handling
            downloadRepository.downloadChart(
                bundleUrl,
                chartId,
                operation,
                onDownloadProgress = { progress ->
                    handleDownloadProgress(chartId, chartName, progress, operation)
                },
                onExtractProgress = { progress ->
                    handleExtractionProgress(chartId, chartName, progress)
                }
            )

            // Success - send complete event and update notification
            downloadServiceConnection.sendEvent(DownloadEvent.Complete(chartId))

            updateNotification(
                chartId = chartId,
                title = finalMessage.title,
                message = finalMessage.message,
                progress = 100,
                isOngoing = false
            )

            Log.d(TAG, "Download completed successfully for chart: $chartId")

        } catch (e: Exception) {
            handleDownloadError(chartId, chartName, e)
        } finally {
            // Clean up notification ID when download finishes (success or failure)
            cleanupNotificationId(chartId)
        }
    }

    private fun handleDownloadProgress(
        chartId: String,
        chartName: String,
        progress: Float,
        operation: OperationType
    ) {
        serviceScope.launch {
            try {
                // Send progress event
                downloadServiceConnection.sendEvent(DownloadEvent.Progress(chartId, progress))

                val progressInt = (progress * 100).coerceIn(0f, 100f).toInt()
                val progressMessage = getProgressMessage(chartName, progressInt, operation)

                updateNotification(
                    chartId = chartId,
                    title = progressMessage.title,
                    message = progressMessage.message,
                    progress = progressInt
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error updating download progress", e)
            }
        }
    }

    private fun handleExtractionProgress(chartId: String, chartName: String, progress: Float) {
        serviceScope.launch {
            try {
                // Send extracting event
                downloadServiceConnection.sendEvent(DownloadEvent.Extracting(chartId, progress))

                val progressInt = (progress * 100).coerceIn(0f, 100f).toInt()
                updateNotification(
                    chartId = chartId,
                    title = getString(R.string.extracting_progress_title, chartName),
                    message = getString(R.string.extracting_progress_description, progressInt),
                    progress = progressInt
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error updating extraction progress", e)
            }
        }
    }

    private fun handleDownloadError(chartId: String, chartName: String, error: Exception) {
        Log.e(TAG, "Download failed for chart: $chartId", error)

        // Categorize error types for better user feedback
        val errorType = categorizeError(error)
        val userFriendlyMessage = getUserFriendlyErrorMessage(error, errorType)

        // Send error event
        serviceScope.launch {
            downloadServiceConnection.sendEvent(
                DownloadEvent.Error(chartId, userFriendlyMessage, errorType)
            )

            // Update notification with error
            updateNotification(
                chartId = chartId,
                title = getString(R.string.download_failed),
                message = getString(R.string.download_failed_for_chart, chartName),
                progress = 0,
                isOngoing = false,
                isError = true
            )
        }
    }

    private fun categorizeError(error: Exception): ErrorType {
        return when (error) {
            is DownloadException -> {
                when {
                    error.message?.contains("HTTP 4") == true -> ErrorType.FILE_NOT_FOUND
                    error.message?.contains("HTTP 5") == true -> ErrorType.SERVER_ERROR
                    error.message?.contains("Network") == true -> ErrorType.NETWORK_ERROR
                    error.message?.contains("Permission") == true -> ErrorType.PERMISSION_DENIED
                    else -> ErrorType.DOWNLOAD_ERROR
                }
            }

            is ExtractionException -> ErrorType.EXTRACTION_ERROR
            is IOException -> {
                when {
                    error.message?.contains("Permission", ignoreCase = true) == true -> ErrorType.PERMISSION_DENIED
                    error.message?.contains("No space", ignoreCase = true) == true -> ErrorType.STORAGE_FULL
                    else -> ErrorType.STORAGE_ERROR
                }
            }

            is SecurityException -> ErrorType.PERMISSION_DENIED
            else -> ErrorType.UNKNOWN
        }
    }

    private fun getUserFriendlyErrorMessage(error: Exception, errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.NETWORK_ERROR -> getString(R.string.error_network_connection)
            ErrorType.FILE_NOT_FOUND -> getString(R.string.error_file_not_found)
            ErrorType.SERVER_ERROR -> getString(R.string.error_server_problem)
            ErrorType.PERMISSION_DENIED -> getString(R.string.error_permission_denied)
            ErrorType.STORAGE_FULL -> getString(R.string.error_storage_full)
            ErrorType.STORAGE_ERROR -> getString(R.string.error_storage_problem)
            ErrorType.EXTRACTION_ERROR -> getString(R.string.error_extraction_failed)
            ErrorType.DOWNLOAD_ERROR -> getString(R.string.error_download_failed)
            ErrorType.UNKNOWN -> error.message ?: getString(R.string.unknown_error)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "DownloadService destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.notification_channel_downloads_name)
        val description = getString(R.string.notification_channel_downloads_description)
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(channelId, name, importance).apply {
            this.description = description
            setShowBadge(false) // Don't clutter app badge
            enableVibration(false) // Silent downloads
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(
        title: String,
        message: String,
        progress: Int,
        isOngoing: Boolean = true,
        isError: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (isError) R.drawable.rounded_error_24 else R.drawable.rounded_download_24)
            .setOngoing(isOngoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(!isOngoing) // Allow dismissing completed/failed downloads

        // Add progress bar for ongoing downloads
        if (isOngoing && !isError) {
            builder.setProgress(100, progress.coerceIn(0, 100), progress == 0)
        }


        return builder.build()
    }

    private suspend fun updateNotification(
        chartId: String,
        title: String,
        message: String,
        progress: Int,
        isOngoing: Boolean = true,
        isError: Boolean = false
    ) {
        try {
            val notification = createNotification(title, message, progress, isOngoing, isError)
            notificationManager.notify(getNotificationId(chartId), notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }

    /**
     * Generate a unique notification ID for a given chart ID
     */
    private suspend fun getNotificationId(chartId: String): Int {
        return notificationIdsLock.withLock {
            chartNotificationIds.getOrPut(chartId) {
                // Assign a new ID and increment for next use
                nextNotificationId++
            }
        }
    }

    /**
     * Clean up the notification ID for a completed or failed download
     */
    private suspend fun cleanupNotificationId(chartId: String) {
        notificationIdsLock.withLock {
            chartNotificationIds.remove(chartId)
        }
    }

    /**
     * Data class to hold download parameters
     */
    private data class DownloadParams(
        val chartId: String,
        val bundleUrl: String,
        val chartName: String,
        val operation: OperationType
    )
}