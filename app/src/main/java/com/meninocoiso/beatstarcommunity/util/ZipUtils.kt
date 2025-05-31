package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Unzips a file to a DocumentFile directory
 *
 * @param zipFile The ZIP file to extract
 * @param context Android context for content resolver access
 * @param onProgress Progress callback (0.0f to 1.0f)
 */
suspend fun DocumentFile.unzipFrom(
    zipFile: File,
    context: Context,
    onProgress: (Float) -> Unit = {}
) = withContext(Dispatchers.IO) {
    require(isDirectory && canWrite()) {
        "Target DocumentFile must be a writable directory"
    }

    Log.d("ZipUtils", "Starting unzip operation")
    Log.d("ZipUtils", "ZIP file: ${zipFile.absolutePath}")
    Log.d("ZipUtils", "Target directory: ${uri.path}")
    Log.d("ZipUtils", "ZIP file exists: ${zipFile.exists()}, Size: ${zipFile.length()} bytes")

    if (!zipFile.exists() || zipFile.length() == 0L) {
        throw IllegalArgumentException("ZIP file doesn't exist or is empty")
    }

    try {
        // Use ZipFile instead of ZipInputStream for more reliable extraction
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().asSequence().toList()
            val totalEntries = entries.size

            Log.d("ZipUtils", "Total entries in ZIP: $totalEntries")

            // Log all entries for debugging
            entries.forEachIndexed { index, entry ->
                val method = when(entry.method) {
                    ZipEntry.STORED -> "STORED"
                    ZipEntry.DEFLATED -> "DEFLATED"
                    else -> "OTHER(${entry.method})"
                }

                Log.d("ZipUtils", "Entry $index: ${entry.name}, " +
                        "size: ${entry.size}, " +
                        "compressed: ${entry.compressedSize}, " +
                        "method: $method")
            }

            // Process each entry
            entries.forEachIndexed { index, entry ->
                val entryName = entry.name

                Log.d("ZipUtils", "Processing ${index+1}/$totalEntries: $entryName")

                if (entry.isDirectory) {
                    // Create directory if needed
                    createDirectoryPath(entryName, this@unzipFrom)
                    Log.d("ZipUtils", "Created directory: $entryName")
                } else {
                    // Create parent directories if needed
                    val parentDirPath = entryName.substringBeforeLast('/', "")
                    val parentDir = if (parentDirPath.isNotEmpty()) {
                        createDirectoryPath(parentDirPath, this@unzipFrom)
                    } else {
                        this@unzipFrom
                    }

                    // Create and write to output file
                    val fileName = entryName.substringAfterLast('/')
                    Log.d("ZipUtils", "Creating file: $fileName in ${parentDir.name}")

                    val outputFile = parentDir.createFile("application/octet-stream", fileName)

                    if (outputFile != null) {
                        context.contentResolver.openOutputStream(outputFile.uri)?.use { outputStream ->
                            // Get input stream for this specific entry
                            zip.getInputStream(entry).use { inputStream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytesRead = 0L

                                // Copy data in chunks
                                while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                }

                                Log.d("ZipUtils", "Extracted $fileName with $totalBytesRead bytes")

                                // Force flush and sync
                                outputStream.flush()
                                if (outputStream is FileOutputStream) {
                                    outputStream.fd.sync()
                                }
                            }
                        } ?: run {
                            Log.e("ZipUtils", "Failed to open output stream for $fileName")
                        }
                    } else {
                        Log.e("ZipUtils", "Failed to create file: $fileName")
                    }
                }

                // Update progress
                onProgress((index + 1).toFloat() / totalEntries)
            }
        }
        Log.d("ZipUtils", "Unzip operation completed successfully")
    } catch (e: Exception) {
        Log.e("ZipUtils", "Unzip failed", e)
        throw e
    }
}

/**
 * Creates a directory path recursively in a DocumentFile
 */
private fun createDirectoryPath(path: String, rootDir: DocumentFile): DocumentFile {
    val pathSegments = path.split('/')
    var currentDir = rootDir

    for (segment in pathSegments) {
        if (segment.isEmpty()) continue

        // Check if directory already exists
        var childDir = currentDir.findFile(segment)

        // Create if it doesn't exist
        if (childDir == null || !childDir.isDirectory) {
            childDir = currentDir.createDirectory(segment)
                ?: throw IOException("Failed to create directory: $segment")
        }

        currentDir = childDir
    }

    return currentDir
}