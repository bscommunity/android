package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

private const val TAG = "StorageUtils"

class StorageUtils {
    companion object {
        fun getChartFolderName(chartId: String): String {
            return chartId.split("-").first()
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
                        Log.e(TAG, "Invalid Document URI")
                        return false
                    }
                    
                    // Check if the permission is still valid
                    val flags = context.contentResolver.persistedUriPermissions
                        .find { it.uri == folderUri }?.let { it.isReadPermission && it.isWritePermission } == true

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
}