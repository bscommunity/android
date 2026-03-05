package com.meninocoiso.bscm.data.parser

import androidx.documentfile.provider.DocumentFile

/**
 * Generic interface for parsing external content metadata from files
 */
interface ContentMetadataParser<T> {
    suspend fun parseMetadata(file: DocumentFile): T?
}

