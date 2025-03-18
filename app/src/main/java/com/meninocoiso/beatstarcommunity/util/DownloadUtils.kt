package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.content.res.Resources.NotFoundException
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
        folderName: String,
        folderUri: Uri,
        subFolders: List<String>? = null,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        onProgress(0f)

        try {
            val rootFolder = getFolderFromUri(folderUri)
                ?: throw IllegalStateException("Could not access or create root folder")

            var destinationFolder = rootFolder

            subFolders?.forEach { subFolderName ->
                destinationFolder = getOrCreateSubfolder(destinationFolder.uri, subFolderName)
            }

            Log.d(TAG, "Destination folder: ${destinationFolder.uri}")

            // Find existing chart folder and delete it if it exists
            val existingChartFolder = destinationFolder.findFile(folderName)
            if (existingChartFolder != null && existingChartFolder.exists()) {
                if (!existingChartFolder.delete()) {
                    throw Exception("Failed to delete existing chart folder")
                }
                Log.d(TAG, "Previous chart version folder deleted")
            }

            // If the folder does not exist, create it
            val chartFolder = existingChartFolder ?: destinationFolder.createDirectory(folderName)
                ?: throw Exception("Failed to create chart folder")

            Log.d(TAG, "Chart folder created: ${chartFolder.uri}")

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

    private fun getFolderFromUri(uri: Uri): DocumentFile? {
        return DocumentFile.fromTreeUri(context, uri)
    }

    private fun getOrCreateSubfolder(uri: Uri, folderName: String): DocumentFile {
        val rootFolder = DocumentFile.fromTreeUri(context, uri)
            ?: throw IllegalStateException("Could not access or create root folder")

        return rootFolder.findFile(folderName) ?: rootFolder.createDirectory(folderName)
            ?: throw IllegalStateException("Could not access or create subfolder")
    }

    suspend fun deleteFolderFromUri(folderName: String, destinationFolderUri: Uri, subFolders: List<String>) {
        withContext(Dispatchers.IO) {
            val rootFolder = getFolderFromUri(destinationFolderUri)
                ?: throw IllegalStateException("Could not access or create root folder")

            var destinationFolder = rootFolder

            subFolders.forEach { subFolderName ->
                destinationFolder = destinationFolder.findFile(subFolderName)
                    ?: throw IllegalStateException("Could not access subfolder: $subFolderName")
            }

            val chartFolder = destinationFolder.findFile(folderName)
                ?: throw NotFoundException("Chart folder not found")

            if (!chartFolder.delete()) {
                throw Exception("Failed to delete chart folder")
            }
        }
    }
}