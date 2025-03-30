package com.meninocoiso.beatstarcommunity.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.repository.DownloadRepository
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.util.StringUtils.Companion.getFinalMessage
import com.meninocoiso.beatstarcommunity.util.StringUtils.Companion.getInitialMessage
import com.meninocoiso.beatstarcommunity.util.StringUtils.Companion.getProgressMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationId = 1001
    private val channelId = "download_channel"

    companion object {
        const val EXTRA_CHART_ID = "extra_chart_id"
        const val EXTRA_CHART_URL = "extra_chart_url"
        const val EXTRA_CHART_NAME = "extra_chart_name"
        const val EXTRA_IS_UPDATE = "extra_is_update"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val chartId = intent?.getStringExtra(EXTRA_CHART_ID)
        val chartUrl = intent?.getStringExtra(EXTRA_CHART_URL)
        val chartName = intent?.getStringExtra(EXTRA_CHART_NAME)
        val operation = if (intent?.getBooleanExtra(EXTRA_IS_UPDATE, false) == true)
            OperationType.UPDATE else OperationType.INSTALL

        if (chartId != null && chartUrl != null && chartName != null) {
            val initialString = getInitialMessage(chartName, operation)
            val finalString = getFinalMessage(chartName, operation)

            Log.d("Operation", operation.toString())

            // Start as foreground service with initial notification
            val notification = createNotification(
                title = initialString.title,
                message = initialString.message,
                progress = 0
            )

            startForeground(notificationId, notification)
            Log.d(TAG, initialString.title)

            // Start the download
            serviceScope.launch {
                try {
                    // Perform the download
                    downloadRepository.downloadChart(
                        chartUrl,
                        chartId,
                        operation,
                        onDownloadProgress = { progress ->
                            // Send progress event
                            serviceScope.launch {
                                downloadServiceConnection.sendEvent(
                                    DownloadEvent.Progress(
                                        chartId,
                                        progress
                                    )
                                )
                            }

                            val progressInt = (progress * 100).toInt()
                            val progressString =
                                getProgressMessage(chartName, progressInt, operation)
                            updateNotification(
                                title = progressString.title,
                                message = progressString.message,
                                progress = progressInt
                            )
                        },
                        onExtractProgress = { progress ->
                            // Send extracting event
                            serviceScope.launch {
                                downloadServiceConnection.sendEvent(
                                    DownloadEvent.Extracting(
                                        chartId,
                                        progress
                                    )
                                )
                            }

                            val progressInt = (progress * 100).toInt()
                            updateNotification(
                                title = "Extracting $chartName",
                                message = "Extracting files... $progressInt%",
                                progress = progressInt
                            )
                        }
                    )

                    // Send complete event
                    downloadServiceConnection.sendEvent(DownloadEvent.Complete(chartId))

                    updateNotification(
                        title = finalString.title,
                        message = finalString.message,
                        progress = 100,
                        isOngoing = false
                    )

                    Log.d(TAG, finalString.title)
                    stopSelf()
                } catch (e: Exception) {
                    // Send error event
                    serviceScope.launch {
                        downloadServiceConnection.sendEvent(
                            DownloadEvent.Error(chartId, e.message ?: "Unknown error")
                        )
                    }

                    updateNotification(
                        title = "Download Failed",
                        message = "Error: ${e.message}",
                        progress = 0,
                        isOngoing = false
                    )

                    Log.e(TAG, "Download failed", e)
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val name = "Chart Downloads"
        val description = "Notifications for chart downloads"
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(channelId, name, importance).apply {
            this.description = description
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(
        title: String,
        message: String,
        progress: Int,
        isOngoing: Boolean = true
    ): android.app.Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.rounded_download_24)
            .setOngoing(isOngoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress > 0) {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    private fun updateNotification(
        title: String,
        message: String,
        progress: Int,
        isOngoing: Boolean = true
    ) {
        val notification = createNotification(title, message, progress, isOngoing)
        notificationManager.notify(notificationId, notification)
    }
}