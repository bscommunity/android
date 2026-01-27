package com.meninocoiso.bscm.data.service

import android.net.Uri

/**
 * Generic interface for scanning installed content from storage
 */
interface ContentStorageScanner<T> {
    suspend fun scanInstalledContent(rootUri: Uri): Map<String, T>
}

