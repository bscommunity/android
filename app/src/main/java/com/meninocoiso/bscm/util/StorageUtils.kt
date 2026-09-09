package com.meninocoiso.bscm.util

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.io.IOException

private const val TAG = "StorageUtils"
private const val BEATSTAR_URI_STRING = "content://com.android.externalstorage.documents/tree/primary%3Abeatstar"
private const val INITIAL_URL_STRING = "content://com.android.externalstorage.documents/document/primary:"

object StorageUtils {
    const val CHART_FOLDER_PREFIX = "bscm_"

    val BEATSTAR_URI: Uri
        get() = BEATSTAR_URI_STRING.toUri()

    // Try to find external storage - typically /storage/emulated/0
    val INITIAL_URL: Uri
        get() = INITIAL_URL_STRING.toUri()

    /**
     * Enhanced DocumentFile extension with better error handling
     */
    internal fun DocumentFile.getOrCreateSubfolder(name: String): DocumentFile {
        return findFile(name) ?: createDirectory(name)
        ?: throw IOException("Failed to create/access subfolder: $name")
    }

    fun getChartFolderName(id: String): String {
        return "$CHART_FOLDER_PREFIX$id"
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

    fun getStoragePermission(context: Context): List<UriPermission> {
        return context.contentResolver.persistedUriPermissions
    }
    
    fun getFolderUri(context: Context, targetUri: Uri): Uri? {
        val permissions = getStoragePermission(context)
        return permissions.find { it.uri == targetUri }?.uri
    }

    fun checkStoragePermission(
        context: Context,
        folderUri: Uri,
    ): Boolean {
        try {
            // Check if the folder exists
            if (!checkIfExists(folderUri, context)) {
                Log.e(TAG, "Invalid Document URI, folder does not exist: $folderUri")
                throw IllegalStateException("Folder does not exist")
            }

            // Check if the permission is still valid
            val flags =
                context.contentResolver.persistedUriPermissions.any {
                    it.uri == BEATSTAR_URI &&
                            it.isReadPermission &&
                            it.isWritePermission
                }

            /*
            * .find { it.uri == folderUri }
                ?.let { it.isReadPermission && it.isWritePermission } == true
            * */

            // .any { it.uri == BEATSTAR_URI && it.isReadPermission && it.isWritePermission }

            if (flags) {
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URI permissions", e)
        }

        return false
    }

    @Composable
    fun folderPickerLauncher(
        context: Context,
        validate: (Uri?) -> Boolean = { true },
        onPermissionGranted: (Uri) -> Unit,
        onInvalidSelection: (() -> Unit)? = null
    ): ActivityResultLauncher<Uri?> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            println("Selected URI: $uri")

            if (!validate(uri)) {
                onInvalidSelection?.invoke()
                return@rememberLauncherForActivityResult
            }

            uri?.let {
                val takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                onPermissionGranted(it)
            }
        }
    }
}