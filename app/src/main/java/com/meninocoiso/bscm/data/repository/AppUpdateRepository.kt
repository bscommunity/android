package com.meninocoiso.bscm.data.repository

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.BuildConfig
import com.meninocoiso.bscm.domain.model.internal.UpdateCache
import com.meninocoiso.bscm.presentation.viewmodel.AppUpdateState
import com.meninocoiso.bscm.data.manager.DownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    private val downloadManager: DownloadManager,
    private val okHttpClient: OkHttpClient,
    private val dataStore: DataStore<Preferences>,
) {
    private val currentVersion = BuildConfig.VERSION_NAME

    companion object AppUpdateKeys {
        val LATEST_UPDATE_VERSION = stringPreferencesKey("app_update_version")
        val LATEST_CLEANED_VERSION = stringPreferencesKey("latest_cleaned_version")
    }

    val appUpdateFlow: Flow<UpdateCache> = dataStore.data
        .catch { exception ->
            when (exception) {
                is IOException -> {
                    Log.e("SettingsRepository", "Error reading preferences", exception)
                    emit(emptyPreferences())
                }
                else -> throw exception
            }
        }
        .map { preferences ->
            mapSettings(preferences)
        }

    suspend fun getLatestVersion(): String {
        return dataStore.data.first()[LATEST_UPDATE_VERSION] ?: ""
    }

    suspend fun setLatestVersion(version: String) {
        dataStore.edit { it[LATEST_UPDATE_VERSION] = version }
    }

    suspend fun getLatestCleanedVersion(): Int {
        return dataStore.data.first()[LATEST_CLEANED_VERSION]?.toIntOrNull() ?: 0
    }

    suspend fun setLatestCleanedVersion(version: Int) {
        dataStore.edit { it[LATEST_CLEANED_VERSION] = version.toString() }
    }

    private fun mapSettings(preferences: Preferences): UpdateCache =
        UpdateCache(
            latestUpdateVersion = preferences[LATEST_UPDATE_VERSION].let {
                if (it.isNullOrEmpty()) null else it
            } ?: UpdateCache().latestUpdateVersion,
            latestCleanedVersion = preferences[LATEST_CLEANED_VERSION]?.toIntOrNull()
                ?: UpdateCache().latestCleanedVersion,
        )
    
    /* Helper Functions */
    
    fun hasUpdate(fetchedVersion: String): Boolean {
        // Log.d(TAG, "Fetched version: $fetchedVersion, Current version: v$currentVersion")
        return fetchedVersion > "v$currentVersion"
    }
    
    /**
     * Helper function to compare versions and update cacheState
     */
    fun getUpdateState(fetchedVersion: String): AppUpdateState {
        // Log.d(TAG, "Fetched version: $fetchedVersion, Current version: $currentVersion")

        return if (hasUpdate(fetchedVersion)) {
            val apkFile = getApkFile(fetchedVersion)

            if (apkFile != null) {
                AppUpdateState.ReadyToInstall(apkFile)
            } else {
                AppUpdateState.UpdateAvailable(fetchedVersion)
            }
        } else {
            AppUpdateState.UpToDate
        }
    }
    
    /**
     * Fetches the latest version from GitHub releases API
     * @return Flow emitting the latest version string
     */
    fun fetchLatestVersion(): Flow<String> = flow {
        val url = "https://api.github.com/repos/bscommunity/android/releases/latest"
        
        Log.d(TAG, "Fetching latest version from $url")
        
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

            // Log.d(TAG, "Latest: $release")
            // Log.d(TAG, "Latest version: ${release.tagName}")

            // Emit the version
            emit(release.tagName)
        }
    }.flowOn(Dispatchers.IO) // Execute network operations on IO dispatcher

    private fun getApkFile(versionName: String): File? {
        val apkFile = File(context.cacheDir, "update-$versionName.apk")
        return if (apkFile.exists()) {
            apkFile
        } else {
            null
        }
    }

    /**
     * Downloads an APK update and prepares it for installation
     * @param versionName Name of the version for display purposes
     * @return The downloaded APK file
     */
    suspend fun downloadApkUpdate(
        versionName: String,
        onProgress: (AppUpdateState) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val downloadUrl = "https://github.com/bscommunity/android/releases/download/${versionName}/app-release.apk"
        
        try {
            onProgress(AppUpdateState.Downloading(0f))

            // Download APK to cache directory with version name
            val apkFile = downloadManager.downloadFileToCache(
                downloadUrl,
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
    suspend fun installApk(apkFile: File) = withContext(Dispatchers.IO) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return@withContext
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    }
}