package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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

/**
 * Utility class for handling chart downloads, extraction and storage
 */
@Singleton
class DownloadUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    /**
     * Downloads a file from a URL to the app's cache directory
     */
    suspend fun downloadFileToCache(
        url: String,
        fileName: String,
        extension: String? = "zip",
        onProgress: (Float) -> Unit
    ): File =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "$fileName.$extension")

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
                                    onProgress(progress)
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
    suspend fun extractZipToFolder(
        zipFile: File,
        destinationFolderUri: Uri,
        chartFolderName: String,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        onProgress(0f)

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

                    Log.d(TAG, "Extracted: $entryName")
                    Log.d(TAG, "total: $totalEntries")
                    Log.d(TAG, "current: $processedEntries")

                    // Update progress
                    processedEntries++
                    onProgress(processedEntries.toFloat() / totalEntries)
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

    fun getFolderFromUri(uri: Uri): DocumentFile? {
        return DocumentFile.fromTreeUri(context, uri)
    }

    suspend fun deleteFolderFromUri(destinationFolderUri: Uri, folderName: String) {
        withContext(Dispatchers.IO) {
            val destinationFolder = getFolderFromUri(destinationFolderUri)
                ?: throw IllegalStateException("Could not find chart folder")

            val chartFolder = destinationFolder.findFile(folderName)
                ?: throw IllegalStateException("Could not find chart folder")

            chartFolder.delete()
        }
    }
}