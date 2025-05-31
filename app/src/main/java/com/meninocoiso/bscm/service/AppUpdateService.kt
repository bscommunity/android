package com.meninocoiso.bscm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.repository.AppUpdateRepository
import com.meninocoiso.bscm.presentation.viewmodel.AppUpdateState
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AppUpdateService"

@AndroidEntryPoint
class AppUpdateService : Service() {

    @Inject
    lateinit var appUpdateRepository: AppUpdateRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationId = 1002
    private val channelId = "app_update_channel"

    companion object {
        private const val EXTRA_UPDATE_URL = "extra_update_url"
        private const val EXTRA_VERSION_NAME = "extra_version_name"

        fun startUpdateDownload(context: Context, updateUrl: String, versionName: String) {
            val intent = Intent(context, AppUpdateService::class.java).apply {
                putExtra(EXTRA_UPDATE_URL, updateUrl)
                putExtra(EXTRA_VERSION_NAME, versionName)
            }

            context.startForegroundService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private var _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    private val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val versionName = intent?.getStringExtra(EXTRA_VERSION_NAME)

        if (versionName != null) {
            // Start as foreground service with initial notification
            val notification = createNotification(
                title = "Downloading Update",
                message = "Starting download for version $versionName...",
                progress = 0
            )
            startForeground(notificationId, notification)
            Log.d(TAG, "Downloading update: $versionName")

            // Start the download
            serviceScope.launch {
                try {
                    // Download the APK
                    val apkFile = appUpdateRepository.downloadApkUpdate(versionName) {
                        _updateState.value = it
                    }

                    // Monitor download cacheState
                    updateState.collectLatest { state ->
                        when (state) {
                            is AppUpdateState.Downloading -> {
                                val progress = (state.progress * 100).toInt()
                                Log.d(TAG, "Downloading update: $progress%")
                                updateNotification(
                                    title = "Downloading Update",
                                    message = "Downloading version $versionName... $progress%",
                                    progress = progress
                                )
                            }
                            is AppUpdateState.ReadyToInstall -> {
                                updateNotification(
                                    title = "Update Ready",
                                    message = "Tap to install version $versionName",
                                    progress = 100,
                                    isOngoing = false,
                                    installIntent = buildInstallPendingIntent(apkFile)
                                )

                                // Start installation
                                try {
                                    appUpdateRepository.installApk(apkFile)
                                } catch (e: Exception) {
                                    Log.e(TAG, "APK installation failed", e)
                                    stopSelf()
                                }

                                // Service will continue running until user responds to install prompt
                            }
                            is AppUpdateState.Error -> {
                                updateNotification(
                                    title = "Update Failed",
                                    message = "Error: ${state.message}",
                                    progress = 0,
                                    isOngoing = false
                                )
                                Log.e(TAG, "Update failed: ${state.message}")
                                stopSelf()
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    updateNotification(
                        title = "Update Failed",
                        message = "Error: ${e.message}",
                        progress = 0,
                        isOngoing = false
                    )
                    Log.e(TAG, "Update download failed", e)
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

    private fun buildInstallPendingIntent(apkFile: File): PendingIntent {
        val installIntent = Intent(this, AppUpdateService::class.java).apply {
            action = "INSTALL_APK"
            putExtra("APK_PATH", apkFile.absolutePath)
        }

        return PendingIntent.getService(
            this,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val name = "App Updates"
        val description = "Notifications for app updates"
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
        isOngoing: Boolean = true,
        installIntent: PendingIntent? = null
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

        if (installIntent != null) {
            builder.setContentIntent(installIntent)
            builder.setAutoCancel(true)
        }

        return builder.build()
    }

    private fun updateNotification(
        title: String,
        message: String,
        progress: Int,
        isOngoing: Boolean = true,
        installIntent: PendingIntent? = null
    ) {
        val notification = createNotification(title, message, progress, isOngoing, installIntent)
        notificationManager.notify(notificationId, notification)
    }
}