package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

class PermissionUtils {
    companion object {
        suspend fun checkStoragePermission(
            getFolderUri: suspend () -> Uri?,
            context: Context
        ): Boolean {
            // Check if we already have a valid folder URI
            val folderUri = getFolderUri()
            if (folderUri != null) {
                try {
                    // Check if the folder exists
                    if (!DocumentFile.isDocumentUri(context, folderUri)) {
                        Log.e("StoragePermission", "Invalid Document URI")
                        return false
                    }
                    
                    // Check if the permission is still valid
                    val flags = context.contentResolver.persistedUriPermissions
                        .find { it.uri == folderUri }?.let { it.isReadPermission && it.isWritePermission } == true

                    if (flags) {
                        return true
                    }
                } catch (e: Exception) {
                    Log.e("StoragePermission", "Error checking URI permissions", e)
                }
            }

            return false
        }
    }
}