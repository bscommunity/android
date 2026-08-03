package com.meninocoiso.bscm.data.manager

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.TourPassLocalRepository
import com.meninocoiso.bscm.domain.repository.TourPassRemoteRepository
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TourPassStorageManager"

private const val MANIFEST_FILE_NAME = "bscm.json"

@Serializable
data class InstalledTourPassEntry(
    val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("chartIds") val chartIds: List<String> = emptyList(),
    @SerialName("installedAt") val installedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class BscmManifest(
    @SerialName("version") val version: Int = 1,
    @SerialName("tourPasses") val tourPasses: List<InstalledTourPassEntry> = emptyList(),
)

/**
 * Persists the "installed tour passes" metadata in a [BscmManifest] stored as
 * `bscm.json` at the root of the beatstar folder, plus a per-chart `bscm.json`
 * inside every chart folder.
 *
 * The manifest survives app uninstalls because it lives on external storage,
 * and it is the source of truth used by the hydration system to re-discover
 * charts and tour passes after a fresh install.
 */
@Singleton
class TourPassStorageManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tourPassLocalRepository: TourPassLocalRepository,
    private val tourPassRemoteRepository: TourPassRemoteRepository,
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Persists the tour pass as installed in the local database and records it
     * in the root manifest. Called after every chart of the tour pass has been
     * downloaded successfully.
     */
    suspend fun addInstalledTourPass(tourPass: TourPass) {
        withContext(Dispatchers.IO) {
            val manifest = readManifest()
            val entry = InstalledTourPassEntry(
                id = tourPass.id,
                name = tourPass.name,
                chartIds = tourPass.charts.map { it.id },
                installedAt = System.currentTimeMillis()
            )
            writeManifest(
                manifest.copy(
                    tourPasses = (manifest.tourPasses.filterNot { it.id == tourPass.id } + entry)
                )
            )
        }
        tourPassLocalRepository.insert(listOf(tourPass.copy(isInstalled = true))).first()
    }

    /**
     * Uninstalls a tour pass: removes it from the root manifest and marks it as
     * not installed in the local database. The actual chart files are deleted
     * by the caller (per-chart delete flow).
     */
    suspend fun removeInstalledTourPass(tourPassId: String) {
        withContext(Dispatchers.IO) {
            val manifest = readManifest()
            writeManifest(
                manifest.copy(tourPasses = manifest.tourPasses.filterNot { it.id == tourPassId })
            )
        }
        val tourPass = tourPassLocalRepository.getTourPass(tourPassId).first().getOrNull()
        if (tourPass != null) {
            tourPassLocalRepository.update(listOf(tourPass.copy(isInstalled = false))).first()
        }
    }

    fun readInstalledTourPasses(): List<InstalledTourPassEntry> = readManifest().tourPasses

    /**
     * Reads the root manifest and fetches the full data of every recorded tour
     * pass from the API, persisting it in the local database so the updates
     * page can show it as installed after a reinstall.
     */
    suspend fun hydrateInstalledTourPasses() {
        val entries = readInstalledTourPasses()
        if (entries.isEmpty()) return

        for (entry in entries) {
            try {
                val result = tourPassRemoteRepository.getTourPass(entry.id).first()
                val tourPass = result.getOrNull() ?: continue
                tourPassLocalRepository.insert(listOf(tourPass.copy(isInstalled = true))).first()
                Log.d(TAG, "Hydrated tour pass ${entry.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hydrate tour pass ${entry.id}", e)
            }
        }
    }

    /**
     * Writes the per-chart `bscm.json` containing the canonical id of a chart
     * into its folder, so the content scanner can identify the chart after a
     * reinstall even when `info.json` is missing.
     *
     * If the chart folder already contains a richer `bscm.json` (e.g. one that
     * ships inside the bundle), its fields are preserved and only the canonical
     * id is ensured, so no metadata is lost on re-download.
     */
    suspend fun writeChartIdFile(folder: DocumentFile, id: String) {
        withContext(Dispatchers.IO) {
            val existing = readChartMetadata(folder)
            val merged = (existing ?: BscmChartMetadata()).copy(chartId = id)
            writeJsonFile(
                parent = folder,
                name = MANIFEST_FILE_NAME,
                content = json.encodeToString(BscmChartMetadata.serializer(), merged)
            )
        }
    }

    /**
     * Reads the `bscm.json` file inside a chart folder, returning its id.
     */
    fun readChartIdFile(folder: DocumentFile): String? = readChartMetadata(folder)?.resolvedId()

    /**
     * Reads the full `bscm.json` metadata inside a chart folder, so the scanner
     * can use the chart's own title/artist/cover/difficulty even when it cannot
     * reach the server or `info.json` is missing.
     */
    fun readChartMetadata(folder: DocumentFile): BscmChartMetadata? {
        return try {
            val file = folder.findFile(MANIFEST_FILE_NAME) ?: return null
            context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                json.decodeFromString<BscmChartMetadata>(reader.readText())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read $MANIFEST_FILE_NAME in ${folder.name}", e)
            null
        }
    }

    private fun readManifest(): BscmManifest {
        return try {
            val root = DocumentFile.fromTreeUri(context, BEATSTAR_URI) ?: return BscmManifest()
            val file = root.findFile(MANIFEST_FILE_NAME) ?: return BscmManifest()
            context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                json.decodeFromString<BscmManifest>(reader.readText())
            } ?: BscmManifest()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read root $MANIFEST_FILE_NAME", e)
            BscmManifest()
        }
    }

    private fun writeManifest(manifest: BscmManifest): Boolean {
        return try {
            val root = DocumentFile.fromTreeUri(context, BEATSTAR_URI) ?: return false
            writeJsonFile(
                parent = root,
                name = MANIFEST_FILE_NAME,
                content = json.encodeToString(BscmManifest.serializer(), manifest)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write root $MANIFEST_FILE_NAME", e)
            false
        }
    }

    private fun writeJsonFile(parent: DocumentFile, name: String, content: String): Boolean {
        return try {
            parent.findFile(name)?.delete()
            val file = parent.createFile("application/json", name) ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $name", e)
            false
        }
    }

    /**
     * Per-chart `bscm.json` payload. Written by the downloader and also shipped
     * inside bundles; carries the canonical `chartId` plus display metadata that
     * lets the scanner identify and render the chart after an app reinstall.
     */
    @Serializable
    data class BscmChartMetadata(
        @SerialName("version") val version: Int = 1,
        @SerialName("chartId") val chartId: String? = null,
        // Legacy files written by older app versions used "id" instead of "chartId".
        @SerialName("id") val legacyId: String? = null,
        @SerialName("track") val track: String? = null,
        @SerialName("artist") val artist: String? = null,
        @SerialName("difficulty") val difficulty: Int? = null,
        @SerialName("isDeluxe") val isDeluxe: Boolean? = null,
        @SerialName("isExplicit") val isExplicit: Boolean? = null,
        @SerialName("bpm") val bpm: Double? = null,
        @SerialName("duration") val duration: Float? = null,
        @SerialName("notes") val notes: Int? = null,
        @SerialName("effects") val effects: Int? = null,
        @SerialName("contributors") val contributors: List<BscmContributor> = emptyList(),
        @SerialName("cover") val cover: String? = null,
    ) {
        fun resolvedId(): String? =
            chartId?.takeIf { it.isNotBlank() } ?: legacyId?.takeIf { it.isNotBlank() }
    }

    @Serializable
    data class BscmContributor(
        @SerialName("username") val username: String? = null,
        @SerialName("avatarUrl") val avatarUrl: String? = null,
        @SerialName("role") val role: String? = null,
    )
}
