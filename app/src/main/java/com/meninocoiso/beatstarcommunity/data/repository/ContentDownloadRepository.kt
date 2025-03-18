package com.meninocoiso.beatstarcommunity.data.repository

import android.content.res.Resources.NotFoundException
import androidx.core.net.toUri
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ContentDownloadRepository"

private const val PLACEHOLDER_FILE_URL = "https://cdn.discordapp.com/attachments/954166390619783268/1348188127780671519/Core.zip?ex=67d9c249&is=67d870c9&hm=b8402284e05d75e729919cf7c0eb0210d346c61e37b9fbc9d72878b8698621dd&"

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
            folderName,
            folderUri,
            listOf("songs"),
            onExtractProgress
        )

        // Clean up temporary files
        downloadedFile.delete()
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
    }

    fun getChartFolderName(chartId: String): String {
        return chartId.split("-").first()
    }
}