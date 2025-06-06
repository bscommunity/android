package com.meninocoiso.bscm.data.repository

import android.content.res.Resources.NotFoundException
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.FetchResult
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.data.manager.DownloadManager
import com.meninocoiso.bscm.util.StorageUtils
import kotlinx.coroutines.flow.first
import kotlinx.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadRepository"

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadManager: DownloadManager,
    private val chartManager: ChartManager,
    private val cacheRepository: CacheRepository,
) {
    /**
     * Downloads and extracts a chart to the beatstar folder
     * @param url URL of the chart zip file
     * @param chartId ID of the chart
     * @param operation Operation type (INSTALL or UPDATE)
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
        val folderUri = cacheRepository.getFolderUri()
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        val folderName = StorageUtils.getChartFolderName(chartId)

        // Download the zip file to cache
        val downloadedFile = downloadManager.downloadFileToCache(
            url,
            folderName,
            "zip",
            onDownloadProgress
        )
        
        // Notify server about the download (this should not block)
        chartManager.postAnalytics(chartId, operation)
        
        // Extract the zip file to the beatstar folder
        try {
            downloadManager.extractZipToFolder(
                downloadedFile,
                folderName,
                folderUri,
                listOf("songs"),
                onExtractProgress
            )
        } catch (e: IOException) {
            throw e
        } finally {
            // Clean up temporary files independently of success
            downloadedFile.delete()
            println("Deleted temporary file: ${downloadedFile.absolutePath}")
        }

        // Update the chart list
        chartManager.updateChart(chartId, operation).first().let {
            if (it is FetchResult.Error) {
                throw Error(it.message)
            }
        }
    }

    suspend fun deleteChart(chartId: String) {
        val folderName = StorageUtils.getChartFolderName(chartId)
        val destinationFolderUri = cacheRepository.getFolderUri()
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        try {
            downloadManager.deleteFolderFromUri(
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
}