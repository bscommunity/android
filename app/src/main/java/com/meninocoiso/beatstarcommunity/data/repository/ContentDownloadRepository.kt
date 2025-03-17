package com.meninocoiso.beatstarcommunity.data.repository

import androidx.core.net.toUri
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ContentDownloadRepository"

private const val PLACEHOLDER_FILE_URL = "https://cdn.discordapp.com/attachments/954166390619783268/1348060352281182208/NDA.zip?ex=67d8a289&is=67d75109&hm=6a061ca23b66676d61f9e2b35bade5a181018485a0ea9d4724b82030a7b5bdf9&"

@Singleton
class ContentDownloadRepository @Inject constructor(
    private val downloadUtils: DownloadUtils,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Downloads and extracts a chart to the beatstar folder
     * @param url URL of the chart zip file
     * @param folderName Name to use for the chart folder
     * @param onDownloadProgress Callback for download progress
     * @param onExtractProgress Callback for extraction progress
     */
    suspend fun downloadChart(
        url: String,
        folderName: String,
        onDownloadProgress: (Float) -> Unit = {},
        onExtractProgress: (Float) -> Unit = {}
    ) {
        val folderUri = settingsRepository.getFolderUri()?.toUri()
            ?: throw IllegalStateException("Could not access or create beatstar folder")

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
            folderUri,
            folderName,
            onExtractProgress
        )

        // Clean up temporary files
        downloadedFile.delete()
    }

    suspend fun deleteChart(chartId: String) {
        val folderName = getChartFolderName(chartId)
        val destinationFolderUri = settingsRepository.getFolderUri()?.toUri()
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        downloadUtils.deleteFolderFromUri(destinationFolderUri, folderName)
    }

    fun getChartFolderName(chartId: String): String {
        return chartId.split("-").first()
    }
}