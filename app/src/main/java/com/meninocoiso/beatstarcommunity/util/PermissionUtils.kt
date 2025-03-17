package com.meninocoiso.beatstarcommunity.util

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

class PermissionUtils {
    companion object {
        @Composable
        fun StoragePermissionHandler(
            onPermissionGranted: () -> Unit,
            getFolderUri: suspend () -> Uri?
        ) {
            val context = LocalContext.current

            // Check if we already have a valid folder URI
            LaunchedEffect(Unit) {
                val folderUri = getFolderUri()
                if (folderUri != null) {
                    try {
                        // Check if the URI is still valid
                        val flags = context.contentResolver.persistedUriPermissions
                            .find { it.uri == folderUri }?.let { it.isReadPermission && it.isWritePermission }
                            ?: false

                        if (flags) {
                            onPermissionGranted()
                            return@LaunchedEffect
                        }
                    } catch (e: Exception) {
                        Log.e("StoragePermission", "Error checking URI permissions", e)
                    }
                }
            }
        }
    }
}