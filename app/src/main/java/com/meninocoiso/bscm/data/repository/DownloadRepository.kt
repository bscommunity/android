package com.meninocoiso.bscm.data.repository

import DownloadEvent
import android.content.Context
import android.content.res.Resources.NotFoundException
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.DownloadManager
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.monitor.DownloadServiceMonitor
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadRepository"

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadManager: DownloadManager,
    private val chartManager: ChartManager,
    private val downloadServiceMonitor: DownloadServiceMonitor,
    @param:ApplicationContext private val context: Context,
) {
    val downloadEvents: SharedFlow<DownloadEvent> = downloadServiceMonitor.observeDownload()
    
    /**
     * Downloads and extracts a chart to the beatstar folder
     * @param url URL of the chart zip file
     * @param contentId ID of the chart
     * @param operation Operation type (INSTALL or UPDATE)
     * @param onDownloadProgress Callback for download progress
     * @param onExtractProgress Callback for extraction progress
     */
    suspend fun downloadChart(
        url: String,
        contentId: String,
        operation: OperationOption,
        onDownloadProgress: (Float) -> Unit = {},
        onExtractProgress: (Float) -> Unit = {}
    ) {
        val folderUri = StorageUtils.getFolderUri(context, BEATSTAR_URI)
            ?: throw IllegalStateException("Could not access or create beatstar folder")
        
        // Download the zip file to cache
        val downloadedFile = downloadManager.downloadFileToCache(
            url,
            contentId,
            "zip",
            onDownloadProgress
        )
        
        // Notify server about the download (this should not block)
        chartManager.postAnalytics(contentId, operation)
        
        // Extract the zip file to the beatstar folder
        try {
            downloadManager.extractZipToFolder(
                downloadedFile,
                contentId,
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
        val updateResult = chartManager.updateContent(contentId, operation)
        if (updateResult is ContentResult.Error) {
            throw Exception(updateResult.message)
        }
    }

    suspend fun deleteChart(contentId: String) {
        val destinationFolderUri = StorageUtils.getFolderUri(context, BEATSTAR_URI)
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        try {
            downloadManager.deleteFolderFromUri(
                StorageUtils.getChartFolderName(contentId),
                destinationFolderUri,
                listOf("songs"),
            )
        } catch (e: NotFoundException) {
            // Folder does not exist, nothing to delete
        }

        // Update the chart list
        val updateResult = chartManager.updateContent(contentId, OperationOption.DELETE)
        if (updateResult is ContentResult.Error) {
            throw Exception(updateResult.message)
        }
    }
}