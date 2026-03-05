package com.meninocoiso.bscm.domain.model.internal

import androidx.documentfile.provider.DocumentFile

/**
 * Generic representation of an installed content entry
 */
data class InstalledContentEntry<T> (
    val contentId: String,
    val metadata: T?,
    val config: Any?,
    val folder: DocumentFile
)