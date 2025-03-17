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
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.data.repository.ContentDownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "DownloadService"

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var contentDownloadRepository: ContentDownloadRepository

    @Inject
    @Named("Local")
    lateinit var localChartRepository: ChartRepository

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
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val chartId = intent?.getStringExtra(EXTRA_CHART_ID)
        val chartUrl = intent?.getStringExtra(EXTRA_CHART_URL)
        val chartName = intent?.getStringExtra(EXTRA_CHART_NAME)

        if (chartId != null && chartUrl != null && chartName != null) {
            // Start as foreground service with initial notification
            val notification = createNotification(
                title = "Downloading $chartName",
                message = "Starting download...",
                progress = 0
            )

            startForeground(notificationId, notification)
            Log.d(TAG, "Downloading $chartName")

            // Start the download
            serviceScope.launch {
                try {
                    // Perform the download
                    contentDownloadRepository.downloadChart(
                        chartUrl,
                        contentDownloadRepository.getChartFolderName(chartId),
                        onDownloadProgress = { progress ->
                            // Send progress event
                            serviceScope.launch {
                                downloadServiceConnection.sendEvent(DownloadEvent.Progress(chartId, progress))
                            }

                            val progressInt = (progress * 100).toInt()
                            updateNotification(
                                title = "Downloading $chartName",
                                message = "Downloading... $progressInt%",
                                progress = progressInt
                            )
                        },
                        onExtractProgress = { progress ->
                            // Send extracting event
                            serviceScope.launch {
                                downloadServiceConnection.sendEvent(DownloadEvent.Extracting(chartId, progress))
                            }

                            val progressInt = (progress * 100).toInt()
                            updateNotification(
                                title = "Extracting $chartName",
                                message = "Extracting files... $progressInt%",
                                progress = progressInt
                            )
                        }
                    )

                    // Update the chart in the local database
                    localChartRepository.updateChart(chartId, true).collect { result ->
                        if (result.isSuccess) {
                            // Send complete event
                            downloadServiceConnection.sendEvent(DownloadEvent.Complete(chartId))

                            updateNotification(
                                title = "Download Complete",
                                message = "$chartName has been downloaded successfully",
                                progress = 100,
                                isOngoing = false
                            )

                            Log.d(TAG, "Download complete: $chartName")
                            stopSelf()
                        }
                    }
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