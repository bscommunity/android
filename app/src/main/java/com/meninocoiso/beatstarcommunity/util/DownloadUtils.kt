package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
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
                    println("Response: $response")
                    if (!response.isSuccessful) {
                        throw NotFoundException("Download failed: ${response.code}")
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

                println("Downloaded file size: ${cacheFile.length()} bytes")

                return@withContext cacheFile
            } catch (e: Exception) {
                // Clean up on failure
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
                throw e
            }
        }
    
    suspend fun extractZipToFolder(
        zipFile: File,
        folderName: String,
        rootUri: Uri,
        subFolders: List<String> = listOf("songs"),
        onProgress: (Float) -> Unit = {}
    ) {
        // Access the root folder using the URI
        val rootFolder = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw IllegalStateException("Failed to access root folder")

        // Create subfolders (ex: "songs/chart1")
        var destination = rootFolder
        for (sub in subFolders) {
            destination = destination.getOrCreateSubfolder(sub)
        }

        // Create (or recreate) the chart folder
        destination.findFile(folderName)?.delete()
        val chartFolder = destination.getOrCreateSubfolder(folderName)

        // Extract the zip file into the chart folder
        chartFolder.unzipFrom(
            zipFile = zipFile,
            context = context,
            onProgress = onProgress
        )
    }

    suspend fun deleteFolderFromUri(folderName: String, destinationFolderUri: Uri, subFolders: List<String>) {
        withContext(Dispatchers.IO) {
            val rootFolder = DocumentFile.fromTreeUri(context, destinationFolderUri)
                ?: throw IllegalStateException("Could not access or create root folder")

            var destinationFolder = rootFolder

            subFolders.forEach { subFolderName ->
                destinationFolder = destinationFolder.findFile(subFolderName)
                    ?: throw NotFoundException("Could not access subfolder: $subFolderName")
            }

            val chartFolder = destinationFolder.findFile(folderName)
                ?: throw NotFoundException("Chart folder not found")

            if (!chartFolder.delete()) {
                throw Exception("Failed to delete chart folder")
            }
        }
    }
}

internal fun DocumentFile.getOrCreateSubfolder(name: String): DocumentFile {
    return findFile(name) ?: createDirectory(name)
    ?: throw IOException("Failed to create/access subfolder: $name")
}