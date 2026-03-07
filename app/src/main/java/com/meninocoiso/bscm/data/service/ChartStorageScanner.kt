package com.meninocoiso.bscm.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.meninocoiso.bscm.data.parser.ChartMetadataParser
import com.meninocoiso.bscm.data.parser.ExternalContentConfig
import com.meninocoiso.bscm.data.parser.ExternalContentMetadata
import com.meninocoiso.bscm.domain.model.internal.InstalledContentEntry
import com.meninocoiso.bscm.util.StorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val TAG = "ChartStorageScanner"

/**
 * Chart-specific implementation of content storage scanner
 */
class ChartStorageScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val metadataParser: ChartMetadataParser
) : ContentStorageScanner<InstalledContentEntry<ExternalContentMetadata>> {

    private val json = Json { ignoreUnknownKeys = true }
    private val ioDispatcher = Dispatchers.IO

    override suspend fun scanInstalledContent(rootUri: Uri): Map<String, InstalledContentEntry<ExternalContentMetadata>> {
        return withContext(ioDispatcher) {
            val entries = mutableMapOf<String, InstalledContentEntry<ExternalContentMetadata>>()
            try {
                val destination = StorageUtils.getFolder(rootUri, listOf("songs"), context)
                Log.d(TAG, "Scanning for installed charts in ${destination.uri}")
                destination.listFiles().forEach { folder ->
                    Log.d(TAG, "Checking folder: ${folder.name} (${folder.uri})")
                    if (!folder.isDirectory) return@forEach

                    val infoFile = folder.findFile("info.json")
                    val configFile = folder.findFile("config.json")
                    val metadata = infoFile?.let { metadataParser.parseMetadata(it) }
                    val config = configFile?.let { readExternalChartConfig(it) }
                    val contentId = normalizeIdentifier(metadata?.contentId)
                    val localId = normalizeIdentifier(metadata?.id)

                    if (infoFile == null) {
                        Log.d(TAG, "Folder ${folder.name} has no info.json")
                    }
                    if (configFile == null) {
                        Log.d(TAG, "Folder ${folder.name} has no config.json")
                    }

                    Log.d(
                        TAG,
                        "Scanned folder ${folder.name}: contentId=${contentId ?: "none"}, metadataId=${localId ?: "none"}, config=${config != null}"
                    )

                    entries[folder.uri.toString()] = InstalledContentEntry(
                        contentId = contentId,
                        metadata = metadata,
                        config = config,
                        folder = folder
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to scan installed charts", e)
                entries.clear()
            }
            entries.toMap()
        }
    }

    private fun readExternalChartConfig(file: DocumentFile): ExternalContentConfig? {
        return try {
            context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                val jsonText = reader.readText()
                json.decodeFromString<ExternalContentConfig>(jsonText)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse config.json for ${file.name}", e)
            null
        }
    }

    companion object {
        internal fun normalizeIdentifier(value: String?): String? = value?.takeIf { it.isNotBlank() }
    }
}
