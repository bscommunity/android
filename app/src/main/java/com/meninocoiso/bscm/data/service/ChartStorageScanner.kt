package com.meninocoiso.bscm.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.meninocoiso.bscm.data.manager.TourPassStorageManager
import com.meninocoiso.bscm.data.manager.TourPassStorageManager.BscmChartMetadata
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
    private val metadataParser: ChartMetadataParser,
    private val tourPassStorageManager: TourPassStorageManager,
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
                    val infoMetadata = infoFile?.let { metadataParser.parseMetadata(it) }
                    val config = configFile?.let { readExternalChartConfig(it) }

                    // The per-chart bscm.json written by the downloader (or
                    // shipped inside the bundle) carries the canonical id and
                    // display metadata and survives app reinstalls, so it is the
                    // preferred identifier even when info.json is missing. Its
                    // metadata is also used to render the chart when info.json
                    // is not present.
                    val bscmMetadata = tourPassStorageManager.readChartMetadata(folder)
                    val manifestId = normalizeIdentifier(bscmMetadata?.resolvedId())
                    val metadata = infoMetadata ?: bscmMetadata?.toExternalContentMetadata()
                    val contentId = normalizeIdentifier(metadata?.contentId) ?: manifestId
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

        /**
         * Maps a bscm.json role name to the role id used by the contributor
         * pipe-separated format consumed by [ContributorParser].
         */
        private val roleIdsByName = mapOf(
            "author" to 0,
            "chart" to 1,
            "charter" to 1,
            "audio" to 2,
            "revision" to 3,
            "effects" to 4,
            "sync" to 5,
            "gameplay" to 6,
            "art" to 7,
            "textures" to 8,
        )

        internal fun roleIdFor(role: String?): Int = roleIdsByName[role?.lowercase()] ?: 0
    }
}

/**
 * Converts the per-chart bscm.json metadata into the generic external metadata
 * shape used by the placeholder factory, so charts can still be rendered from
 * the manifest alone when info.json is missing or the server is unreachable.
 */
private fun BscmChartMetadata.toExternalContentMetadata(): ExternalContentMetadata? {
    val id = resolvedId() ?: return null
    return ExternalContentMetadata(
        title = track?.takeIf { it.isNotBlank() } ?: id,
        artist = artist ?: "",
        id = id,
        difficulty = difficulty,
        bpm = bpm,
        contentId = id,
        duration = duration,
        notes = notes,
        effects = effects,
        contributors = contributors.mapNotNull { contributor ->
            val username = contributor.username?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            "$username|${contributor.avatarUrl ?: ""}|${ChartStorageScanner.roleIdFor(contributor.role)}"
        }.joinToString("||").takeIf { it.isNotBlank() },
        cover = cover,
    )
}
