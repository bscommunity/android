package com.meninocoiso.beatstarcommunity.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.util.DownloadState
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadUtils: DownloadUtils

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationId = 1001
    private val channelId = "download_channel"

    companion object {
        private const val EXTRA_CHART_URL = "extra_chart_url"
        private const val EXTRA_CHART_NAME = "extra_chart_name"

        fun startDownload(context: Context, chartUrl: String, chartName: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_CHART_URL, chartUrl)
                putExtra(EXTRA_CHART_NAME, chartName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val chartUrl = intent?.getStringExtra(EXTRA_CHART_URL)
        val chartName = intent?.getStringExtra(EXTRA_CHART_NAME)

        println("chartUrl: $chartUrl")
        println("chartName: $chartName")

        if (chartUrl != null && chartName != null) {
            // Start as foreground service with initial notification
            val notification = createNotification(
                title = "Downloading $chartName",
                message = "Starting download...",
                progress = 0
            )
            startForeground(notificationId, notification)

            // Start the download
            serviceScope.launch {
                // Perform the download
                downloadUtils.downloadChart(chartUrl, chartName)

                try {
                    // Monitor download state
                    downloadUtils.downloadState.collectLatest { state ->
                        when (state) {
                            is DownloadState.Downloading -> {
                                val progress = (state.progress * 100).toInt()
                                updateNotification(
                                    title = "Downloading $chartName",
                                    message = "Downloading... $progress%",
                                    progress = progress
                                )
                                println("Downloading... $progress%")
                            }
                            is DownloadState.Extracting -> {
                                val progress = (state.progress * 100).toInt()
                                updateNotification(
                                    title = "Extracting $chartName",
                                    message = "Extracting files... $progress%",
                                    progress = progress
                                )
                                println("Extracting files... $progress%")
                            }
                            is DownloadState.Completed -> {
                                updateNotification(
                                    title = "Download Complete",
                                    message = "$chartName has been downloaded successfully",
                                    progress = 100,
                                    isOngoing = false
                                )
                                println("$chartName has been downloaded successfully")
                                stopSelf()
                            }
                            is DownloadState.Error -> {
                                updateNotification(
                                    title = "Download Failed",
                                    message = "Error: ${state.message}",
                                    progress = 0,
                                    isOngoing = false
                                )
                                println("Error: ${state.message}")
                                stopSelf()
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    updateNotification(
                        title = "Download Failed",
                        message = "Error: ${e.message}",
                        progress = 0,
                        isOngoing = false
                    )
                    println("Error: ${e.message}")
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
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chart Downloads"
            val description = "Notifications for chart downloads"
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(channelId, name, importance).apply {
                this.description = description
            }

            notificationManager.createNotificationChannel(channel)
        }
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