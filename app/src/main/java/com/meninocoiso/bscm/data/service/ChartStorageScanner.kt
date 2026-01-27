package com.meninocoiso.bscm.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.meninocoiso.bscm.data.model.InstalledContentEntry
import com.meninocoiso.bscm.data.parser.ChartMetadataParser
import com.meninocoiso.bscm.data.parser.ExternalContentConfig
import com.meninocoiso.bscm.data.parser.ExternalContentMetadata
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

    override suspend fun scanInstalledContent(rootUri: Uri): Pair<Map<String, InstalledContentEntry<ExternalContentMetadata>>, Int> {
        var totalCharts = 0

        return withContext(ioDispatcher) {
            val entries = mutableMapOf<String, InstalledContentEntry<ExternalContentMetadata>>()
            try {
                val destination = StorageUtils.getFolder(rootUri, listOf("songs"), context)
                destination.listFiles().forEach { folder ->
                    if (!folder.isDirectory) return@forEach
                    val infoFile = folder.findFile("info.json") ?: return@forEach
                    val configFile = folder.findFile("config.json") ?: return@forEach
                    
                    val metadata = metadataParser.parseMetadata(infoFile)
                    val config = readExternalChartConfig(configFile)
                    val chartId = metadata?.id

                    Log.d(TAG, "Found chart folder: ${folder.name} - id: $chartId")
                    
                    if (!chartId.isNullOrBlank()) {
                        totalCharts++

                        entries[chartId] = InstalledContentEntry(
                            contentId = chartId,
                            metadata = metadata,
                            config = config,
                            folder = folder
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to scan installed charts", e)
                entries.clear()
            }

            Pair(entries, totalCharts)
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
}

