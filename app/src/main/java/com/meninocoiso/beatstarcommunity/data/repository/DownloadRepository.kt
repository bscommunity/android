package com.meninocoiso.beatstarcommunity.data.repository

import android.content.res.Resources.NotFoundException
import androidx.core.net.toUri
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.FetchResult
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadRepository"

private const val PLACEHOLDER_FILE_URL = "https://cdn.discordapp.com/attachments/954166390619783268/1351961718250536960/heretostay.zip?ex=67e8ce37&is=67e77cb7&hm=1035475821cb2771bef8b26e153f2c945642190105c40f6d351b93af87c7f65e&"

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadUtils: DownloadUtils,
    private val chartManager: ChartManager,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Downloads and extracts a chart to the beatstar folder
     * @param url URL of the chart zip file
     * @param chartId ID of the chart
     * @param operation Operation type (INSTALL or UPDATE)
     * @param folderName Name to use for the chart folder
     * @param onDownloadProgress Callback for download progress
     * @param onExtractProgress Callback for extraction progress
     */
    suspend fun downloadChart(
        url: String,
        chartId: String,
        operation: OperationType,
        onDownloadProgress: (Float) -> Unit = {},
        onExtractProgress: (Float) -> Unit = {}
    ) {
        val folderUri = settingsRepository.getFolderUri()?.toUri()
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        val folderName = getChartFolderName(chartId)

        // Download the zip file to cache
        val downloadedFile = downloadUtils.downloadFileToCache(
            PLACEHOLDER_FILE_URL,
            folderName,
            ".zip",
            onDownloadProgress
        )

        // Extract the zip file to the beatstar folder
        downloadUtils.extractZipToFolder(
            downloadedFile,
            folderName,
            folderUri,
            listOf("songs"),
            onExtractProgress
        )

        // Clean up temporary files
        downloadedFile.delete()

        // Update the chart list
        chartManager.updateChart(chartId, operation).first().let {
            if (it is FetchResult.Error) {
                throw Error(it.message)
            }
        }
    }

    suspend fun deleteChart(chartId: String) {
        val folderName = getChartFolderName(chartId)
        val destinationFolderUri = settingsRepository.getFolderUri()?.toUri()
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        try {
            downloadUtils.deleteFolderFromUri(
                folderName,
                destinationFolderUri,
                listOf("songs"),
            )
        } catch (e: NotFoundException) {
            // Folder does not exist, nothing to delete
        }

        // Update the chart list
        chartManager.updateChart(chartId, OperationType.DELETE).first().let {
            if (it is FetchResult.Error) {
                throw Error(it.message)
            }
        }
    }

    private fun getChartFolderName(chartId: String): String {
        return chartId.split("-").first()
    }
}