package com.meninocoiso.bscm.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.io.IOException

private const val TAG = "StorageUtils"

object StorageUtils {
    /**
     * Enhanced DocumentFile extension with better error handling
     */
    internal fun DocumentFile.getOrCreateSubfolder(name: String): DocumentFile {
        return findFile(name) ?: createDirectory(name)
        ?: throw IOException("Failed to create/access subfolder: $name")
    }
    
    fun getChartFolderName(chartId: String): String {
        // Last 4 numbers from the chart ID
        return "bscm_" + chartId.takeLast(4)
    }

    fun checkIfExists(uri: Uri, context: Context): Boolean {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile?.exists() == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URI existence", e)
            false
        }
    }

    fun getFolder(rootUri: Uri, subFolders: List<String>, context: Context): DocumentFile {
        // Access the root folder using the URI
        val rootFolder = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw IllegalStateException("Failed to access root folder")

        // Create subfolders (ex: "songs/chart1")
        var destination = rootFolder
        for (sub in subFolders) {
            destination = destination.getOrCreateSubfolder(sub)
        }

        return destination
    }

    suspend fun checkStoragePermission(
        getFolderUri: suspend () -> Uri?,
        context: Context
    ): Boolean {
        // Check if we already have a valid folder URI
        val folderUri = getFolderUri()
        if (folderUri != null) {
            try {
                // Check if the folder exists
                if (!checkIfExists(folderUri, context)) {
                    Log.e(TAG, "Invalid Document URI, folder does not exist")
                    throw IllegalStateException("Folder does not exist")
                }

                // Check if the permission is still valid
                val flags = context.contentResolver.persistedUriPermissions
                    .find { it.uri == folderUri }
                    ?.let { it.isReadPermission && it.isWritePermission } == true

                if (flags) {
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking URI permissions", e)
            }
        }

        return false
    }
}