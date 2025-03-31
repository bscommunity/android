package com.meninocoiso.beatstarcommunity.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.AppUpdateState
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    // @SerialName("name") val name: String = "",
    // @SerialName("html_url") val htmlUrl: String = ""
)

private val json = Json { ignoreUnknownKeys = true }

private const val TAG = "AppUpdateRepository"

@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadUtils: DownloadUtils,
    private val okHttpClient: OkHttpClient
) {
    /**
     * Fetches the latest version from GitHub releases API
     * @return Flow emitting the latest version string
     */
    fun fetchLatestVersion(): Flow<String> = flow {
        val url = "https://api.github.com/repos/bscommunity/android/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("API request failed with code ${response.code}")
            }

            val responseBody = response.body?.string()
                ?: throw IOException("Empty response body")

            // Parse the JSON response
            val release = json.decodeFromString<GitHubRelease>(responseBody)

            // Emit the version
            emit(release.tagName)
        }
    }.flowOn(Dispatchers.IO) // Execute network operations on IO dispatcher

    fun getApkFile(versionName: String): File? {
        val apkFile = File(context.cacheDir, "update-$versionName.apk")
        return if (apkFile.exists()) {
            apkFile
        } else {
            null
        }
    }

    /**
     * Downloads an APK update and prepares it for installation
     * @param url URL of the APK file
     * @param versionName Name of the version for display purposes
     * @return The downloaded APK file
     */
    suspend fun downloadApkUpdate(
        versionName: String,
        onProgress: (AppUpdateState) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val downloadUrl = "https://github.com/bscomunnity/android/releases/download/${versionName}/bscm-${versionName}.apk"
        
        try {
            onProgress(AppUpdateState.Downloading(0f))

            // Download APK to cache directory with version name
            val apkFile = downloadUtils.downloadFileToCache(
                "${downloadUrl}_$versionName",
                "update-$versionName",
                "apk"
            ) { progress ->
                onProgress(AppUpdateState.Downloading(progress))
            }

            onProgress(AppUpdateState.ReadyToInstall(apkFile))

            return@withContext apkFile
        } catch (e: Exception) {
            Log.e(TAG, "APK download failed", e)
            onProgress(AppUpdateState.Error(e.message ?: "Unknown error"))
            throw e
        }
    }

    /**
     * Triggers the APK installation process
     * @param apkFile The APK file to install
     */
    fun installApk(apkFile: File) {
        // Create content URI for the APK
        val apkUri =
            // For Android 7.0+ we need to use FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

        // Create intent to install the APK
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the installation activity
        context.startActivity(installIntent)
    }
}