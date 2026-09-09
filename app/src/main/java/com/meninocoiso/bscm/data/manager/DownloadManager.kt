package com.meninocoiso.bscm.data.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.meninocoiso.bscm.domain.exceptions.DeletionException
import com.meninocoiso.bscm.domain.exceptions.DownloadException
import com.meninocoiso.bscm.domain.exceptions.ExtractionException
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.ZipUtils.unzipFrom
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadManager"
private const val BUFFER_SIZE = 8192
private const val MIN_FILE_SIZE = 1024L // 1KB minimum for valid files

/**
 * Utility class for handling chart downloads, extraction and storage
 */
@Singleton
class DownloadManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
    ): File = withContext(Dispatchers.IO) {
        val sanitizedFileName = sanitizeFileName(fileName)
        val cacheFile = File(context.cacheDir, "$sanitizedFileName.$extension")
        val tempFile = File(context.cacheDir, "${sanitizedFileName}_temp.$extension")

        try {
            // Clean up any existing files
            listOf(cacheFile, tempFile).forEach { file ->
                if (file.exists() && !file.delete()) {
                    Log.w(TAG, "Failed to delete existing file: ${file.name}")
                }
            }

            // Validate URL
            if (!isValidUrl(url)) {
                throw IllegalArgumentException("Invalid URL provided: $url")
            }

            // Create the request with proper headers
            val request = Request.Builder()
                .url(url)
                .build()

            // Execute the request with proper resource management
            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "Response code: ${response.code} for URL: $url")

                if (!response.isSuccessful) {
                    throw DownloadException("Download failed with HTTP ${response.code}: ${response.message}")
                }

                val responseBody = response.body

                val contentLength = responseBody.contentLength()
                Log.d(TAG, "Content length: $contentLength bytes")

                // Validate content length
                if (contentLength != -1L && contentLength < MIN_FILE_SIZE) {
                    throw DownloadException("File too small: $contentLength bytes")
                }

                // Download to temporary file first
                downloadToFile(responseBody, tempFile, contentLength, onProgress)

                // Validate downloaded file
                validateDownloadedFile(tempFile, contentLength)

                // Move temp file to final location atomically
                if (!tempFile.renameTo(cacheFile)) {
                    throw DownloadException("Failed to move temporary file to final location")
                }

                Log.d(TAG, "Successfully downloaded file: ${cacheFile.name} (${cacheFile.length()} bytes)")
                return@withContext cacheFile
            }
        } catch (e: Exception) {
            // Clean up any temporary or partial files
            cleanupFiles(listOf(cacheFile, tempFile))

            // Re-throw with more context
            when (e) {
                is DownloadException -> throw e
                is IOException -> throw DownloadException("Network error during download", e)
                is SecurityException -> throw DownloadException("Permission denied", e)
                else -> throw DownloadException("Unexpected error during download: ${e.message}", e)
            }
        }
    }

    /**
     * Downloads response body to file with progress tracking
     */
    private fun downloadToFile(
        responseBody: ResponseBody,
        targetFile: File,
        contentLength: Long,
        onProgress: (Float) -> Unit
    ) {
        FileOutputStream(targetFile).use { outputStream ->
            responseBody.source().inputStream().use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesRead: Long = 0
                var lastProgressUpdate = 0f

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Update progress, but not too frequently to avoid UI spam
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                        if (progress - lastProgressUpdate >= 0.01f || progress >= 1.0f) {
                            onProgress(progress)
                            lastProgressUpdate = progress
                        }
                    }
                }

                // Ensure final progress is reported
                if (contentLength > 0) {
                    onProgress(1.0f)
                }
            }
        }
    }

    /**
     * Validates the downloaded file for basic integrity
     */
    private fun validateDownloadedFile(file: File, expectedSize: Long) {
        if (!file.exists()) {
            throw DownloadException("Downloaded file does not exist")
        }

        val actualSize = file.length()
        if (actualSize < MIN_FILE_SIZE) {
            throw DownloadException("Downloaded file is too small: $actualSize bytes")
        }

        if (expectedSize != -1L && actualSize != expectedSize) {
            throw DownloadException("File size mismatch. Expected: $expectedSize, Actual: $actualSize")
        }

        // Basic ZIP file validation if it's supposed to be a ZIP
        if (file.name.endsWith(".zip", ignoreCase = true)) {
            validateZipFile(file)
        }
    }

    /**
     * Basic ZIP file header validation
     */
    private fun validateZipFile(file: File) {
        try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val bytesRead = input.read(header)
                if (bytesRead < 4) {
                    throw DownloadException("File is too small to be a valid ZIP")
                }

                // Check for ZIP file signature (PK\003\004 or PK\005\006 for empty archives)
                val isValidZip = (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                        ((header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) ||
                                (header[2] == 0x05.toByte() && header[3] == 0x06.toByte())))

                if (!isValidZip) {
                    throw DownloadException("Downloaded file is not a valid ZIP archive")
                }
            }
        } catch (e: IOException) {
            throw DownloadException("Failed to validate ZIP file", e)
        }
    }

    suspend fun extractZipToFolder(
        zipFile: File,
        id: String,
        rootUri: Uri,
        subFolders: List<String> = listOf("songs"),
        onProgress: (Float) -> Unit = {}
    ): DocumentFile = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (!zipFile.exists() || !zipFile.canRead()) {
                throw ExtractionException("ZIP file does not exist or is not readable: ${zipFile.path}")
            }

            val sanitizedFolderName = sanitizeFileName(StorageUtils.getChartFolderName(id))
            val destination = StorageUtils.getFolder(rootUri, subFolders, context)

            // Create (or recreate) the chart folder
            destination.findFile(sanitizedFolderName)?.let { folder ->
                if (!folder.delete()) {
                    throw ExtractionException("Failed to delete existing folder: $sanitizedFolderName")
                }
            }

            val chartFolder = destination.createDirectory(sanitizedFolderName)
                ?: throw ExtractionException("Failed to create chart folder: $sanitizedFolderName")

            // Extract the zip file into the chart folder
            try {
                chartFolder.unzipFrom(
                    zipFile = zipFile,
                    context = context,
                    onProgress = onProgress
                )
            } catch (e: Exception) {
                // Clean up partial extraction on failure
                try {
                    chartFolder.delete()
                } catch (cleanupException: Exception) {
                    Log.w(TAG, "Failed to clean up partial extraction", cleanupException)
                }
                throw ExtractionException("Failed to extract ZIP file", e)
            }

            chartFolder
        } catch (e: ExtractionException) {
            throw e
        } catch (e: Exception) {
            throw ExtractionException("Unexpected error during extraction: ${e.message}", e)
        }
    }

    suspend fun deleteFolderFromUri(
        id: String,
        destinationFolderUri: Uri,
        subFolders: List<String>
    ) = withContext(Dispatchers.IO) {
        try {
            val sanitizedFolderName = sanitizeFileName(StorageUtils.getChartFolderName(id))
            val rootFolder = DocumentFile.fromTreeUri(context, destinationFolderUri)
                ?: throw DeletionException("Could not access root folder")

            var currentFolder = rootFolder

            // Navigate to the target subfolder
            for (subFolderName in subFolders) {
                currentFolder = currentFolder.findFile(subFolderName)
                    ?: return@withContext // Folder does not exist, nothing to delete
            }

            val chartFolder = currentFolder.findFile(sanitizedFolderName)
                ?: return@withContext // Folder does not exist, nothing to delete

            if (!chartFolder.delete()) {
                throw DeletionException("Failed to delete chart folder: $sanitizedFolderName")
            }

            Log.d(TAG, "Successfully deleted folder: $sanitizedFolderName")
        } catch (e: DeletionException) {
            throw e
        } catch (e: Exception) {
            throw DeletionException("Unexpected error during folder deletion: ${e.message}", e)
        }
    }

    /**
     * Utility functions for better error handling and validation
     */
    private fun sanitizeFileName(fileName: String): String {
        // Remove or replace invalid characters for file names
        return fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(255) // Limit length to prevent filesystem issues
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val parsed = java.net.URL(url)
            parsed.protocol in listOf("http", "https")
        } catch (_: Exception) {
            false
        }
    }

    private fun cleanupFiles(files: List<File>) {
        files.forEach { file ->
            try {
                if (file.exists() && !file.delete()) {
                    Log.w(TAG, "Failed to delete file during cleanup: ${file.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error during file cleanup", e)
            }
        }
    }
}