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
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ContentDownloadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationId = 1001
    private val channelId = "download_channel"

    companion object {
        private const val EXTRA_CHART_ID = "extra_chart_id"
        private const val EXTRA_CHART_URL = "extra_chart_url"
        private const val EXTRA_CHART_NAME = "extra_chart_name"

        fun startDownload(context: Context, chartId: String, chartUrl: String, chartName: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_CHART_ID, chartId)
                putExtra(EXTRA_CHART_URL, chartUrl)
                putExtra(EXTRA_CHART_NAME, chartName)
            }

            context.startForegroundService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private var _downloadState = MutableStateFlow<ContentDownloadState>(ContentDownloadState.Idle)
    private val downloadState: StateFlow<ContentDownloadState> = _downloadState.asStateFlow()

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
                // Perform the download
                contentDownloadRepository.downloadChart(
                    chartUrl,
                    contentDownloadRepository.getChartFolderName(chartId)
                ) {
                    _downloadState.value = it
                }

                try {
                    // Monitor download state
                    downloadState.collectLatest { state ->
                        when (state) {
                            is ContentDownloadState.Downloading -> {
                                val progress = (state.progress * 100).toInt()
                                Log.d(TAG, "Downloading $chartName: $progress%")
                                updateNotification(
                                    title = "Downloading $chartName",
                                    message = "Downloading... $progress%",
                                    progress = progress
                                )
                            }
                            is ContentDownloadState.Extracting -> {
                                val progress = (state.progress * 100).toInt()
                                Log.d(TAG, "Extracting $chartName: $progress%")
                                updateNotification(
                                    title = "Extracting $chartName",
                                    message = "Extracting files... $progress%",
                                    progress = progress
                                )
                            }
                            is ContentDownloadState.Installed -> {
                                updateNotification(
                                    title = "Download Complete",
                                    message = "$chartName has been downloaded successfully",
                                    progress = 100,
                                    isOngoing = false
                                )

                                localChartRepository.updateChart(chartId, true)
                                    .collect {
                                        if (it.isSuccess) {
                                            _downloadState.value = ContentDownloadState.Installed
                                            Log.d(TAG, "Download complete: $chartName")
                                        }
                                    }

                                stopSelf()
                            }
                            is ContentDownloadState.Error -> {
                                updateNotification(
                                    title = "Download Failed",
                                    message = "Error: ${state.message}",
                                    progress = 0,
                                    isOngoing = false
                                )
                                Log.e(TAG, "Download failed: ${state.message}")
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