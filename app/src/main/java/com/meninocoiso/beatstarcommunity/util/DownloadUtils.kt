package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadUtils"
private const val BEATSTAR_FOLDER_NAME = "beatstar"

/**
 * Download state for tracking download progress
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Extracting(val progress: Float) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * Utility class for handling chart downloads, extraction and storage
 */
@Singleton
class DownloadUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val settingsRepository: SettingsRepository
) {
    // Internal state management
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: Flow<DownloadState> = _downloadState.asStateFlow()

    /**
     * Sets the folder URI in the settings repository.
     *
     * @param uri The URI of the folder to be set.
     */
    suspend fun setFolderUri(uri: String) {
        settingsRepository.setFolderUri(uri)
    }

    /**
     * Retrieves the folder URI from the settings repository.
     *
     * @return The URI of the folder, or null if not set.
     */
    suspend fun getFolderUri(): String? {
        return settingsRepository.getFolderUri()
    }

    /**
     * Downloads and extracts a chart to the beatstar folder
     * @param url URL of the chart zip file
     * @param chartName Name to use for the chart folder
     */
    suspend fun downloadChart(url: String, chartName: String) {
        // Reset state
        _downloadState.value = DownloadState.Idle

        try {
            _downloadState.value = DownloadState.Downloading(0f)

            // Get or create the beatstar folder path
            val beatstarFolderUri = getBeatstarFolderUri()
                ?: throw IllegalStateException("Could not access or create beatstar folder")

            // Download the zip file to cache
            val downloadedFile = downloadFileToCache(url, chartName)

            // Extract the zip file to the beatstar folder
            extractZipToFolder(downloadedFile, beatstarFolderUri, chartName)

            // Clean up temporary files
            downloadedFile.delete()

            _downloadState.value = DownloadState.Completed
        } catch (e: Exception) {
            Log.e(TAG, "Chart download failed", e)
            _downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Gets or creates the beatstar folder URI
     */
    private suspend fun getBeatstarFolderUri(): Uri? = withContext(Dispatchers.IO) {
        try {
            val savedFolderUri = settingsRepository.getFolderUri()

            if (!savedFolderUri.isNullOrEmpty()) {
                val savedUri = Uri.parse(savedFolderUri)

                val destinationFolder = DocumentFile.fromTreeUri(context, savedUri)
                destinationFolder?.listFiles()

                return@withContext savedUri
            }

            // If no saved URI or it fails, create a new folder
            return@withContext createBeatstarFolder()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get or use saved folder, creating new", e)
            return@withContext createBeatstarFolder()
        }
    }

    /**
     * Creates the beatstar folder and returns its URI
     * This handles both direct file access (API < 30) and Storage Access Framework (API >= 30)
     */
    private suspend fun createBeatstarFolder(): Uri? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11+ we should use SAF
                // Need to request user permission through UI first
                return@withContext null // This should be handled by UI flow
            } else {
                // For older Android versions, we can create the folder directly
                val beatstarFolder = File(
                    Environment.getExternalStorageDirectory(),
                    BEATSTAR_FOLDER_NAME
                )

                if (!beatstarFolder.exists()) {
                    if (!beatstarFolder.mkdirs()) {
                        Log.e(TAG, "Failed to create beatstar directory")
                        return@withContext null
                    }
                }

                val uri = Uri.fromFile(beatstarFolder)
                settingsRepository.setFolderUri(uri.toString())
                return@withContext uri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create beatstar folder", e)
            return@withContext null
        }
    }

    /**
     * Downloads a file from a URL to the app's cache directory
     */
    private suspend fun downloadFileToCache(url: String, fileName: String): File =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "$fileName.zip")

            try {
                // Clear any existing file
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }

                // Create the request
                val request = Request.Builder()
                    .url(url)
                    .build()

                // Execute the request
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Download failed: ${response.code}")
                    }

                    val responseBody = response.body ?: throw Exception("Empty response body")
                    val contentLength = responseBody.contentLength()

                    // Create output stream and buffer
                    FileOutputStream(cacheFile).use { outputStream ->
                        responseBody.source().inputStream().use { inputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead: Long = 0

                            // Read and write the file with progress updates
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                if (contentLength > 0) {
                                    val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                    _downloadState.value = DownloadState.Downloading(progress)
                                }
                            }
                        }
                    }
                }

                return@withContext cacheFile
            } catch (e: Exception) {
                // Clean up on failure
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
                throw e
            }
        }

    /**
     * Extracts a zip file to the specified folder
     */
    private suspend fun extractZipToFolder(
        zipFile: File,
        destinationFolderUri: Uri,
        chartFolderName: String
    ) = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Extracting(0f)

        println(DocumentsContract.isDocumentUri(context, destinationFolderUri))
        println(destinationFolderUri.path)

        try {
            val destinationFolder = DocumentFile.fromTreeUri(context, destinationFolderUri)

            // Make sure we have the destination folder
            if (destinationFolder == null || !destinationFolder.exists()) {
                throw Exception("Destination folder does not exist or is not accessible")
            }

            // Create or get chart subfolder
            val chartFolder = destinationFolder.findFile(chartFolderName)
                ?: destinationFolder.createDirectory(chartFolderName)
                ?: throw Exception("Failed to create chart folder")

            // Count entries for progress tracking
            val totalEntries = countZipEntries(zipFile)
            var processedEntries = 0

            // Extract the zip file
            ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
                var entry: ZipEntry? = zipInputStream.nextEntry
                val buffer = ByteArray(8192)

                while (entry != null) {
                    val entryName = entry.name.substringAfterLast('/').replace("\\", "")
                        .lowercase(Locale.getDefault())

                    if (!entry.isDirectory && entryName.isNotEmpty()) {
                        // Create the file in the destination folder
                        val fileType = getMimeType(entryName)
                        val outputFile = chartFolder.createFile(fileType, entryName)
                            ?: throw Exception("Failed to create file: $entryName")

                        // Write the file content
                        context.contentResolver.openOutputStream(outputFile.uri)?.use { outputStream ->
                            var bytesRead: Int
                            while (zipInputStream.read(buffer).also { bytesRead = it } > 0) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        } ?: throw Exception("Failed to open output stream for: $entryName")
                    }

                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry

                    // Update progress
                    processedEntries++
                    _downloadState.value = DownloadState.Extracting(processedEntries.toFloat() / totalEntries)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting zip file", e)
            throw e
        }
    }

    /**
     * Count the number of entries in a zip file for progress tracking
     */
    private suspend fun countZipEntries(zipFile: File): Int = withContext(Dispatchers.IO) {
        var count = 0
        ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
            while (zipInputStream.nextEntry != null) {
                count++
                zipInputStream.closeEntry()
            }
        }
        count
    }

    /**
     * Get MIME type based on file extension
     */
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            fileName.endsWith(".json", true) -> "application/json"
            fileName.endsWith(".txt", true) -> "text/plain"
            fileName.endsWith(".mp3", true) -> "audio/mpeg"
            fileName.endsWith(".ogg", true) -> "audio/ogg"
            fileName.endsWith(".wav", true) -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    /**
     * Reset download state to idle
     */
    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }
}