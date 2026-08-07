package com.meninocoiso.bscm.data.repository

import DownloadEvent
import android.content.Context
import android.content.res.Resources.NotFoundException
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.DownloadManager
import com.meninocoiso.bscm.data.manager.TourPassStorageManager
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.UiText
import com.meninocoiso.bscm.monitor.DownloadServiceMonitor
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadManager: DownloadManager,
    private val chartManager: ChartManager,
    private val tourPassStorageManager: TourPassStorageManager,
    downloadServiceMonitor: DownloadServiceMonitor,
    @param:ApplicationContext private val context: Context,
) {
    val downloadEvents: SharedFlow<DownloadEvent> = downloadServiceMonitor.observeDownload()

    /**
     * Downloads and extracts a chart to the beatstar folder
     * @param url URL of the chart zip file
     * @param id Chart id used as local primary key and preferred /songs folder name
     * @param operation Operation type (INSTALL or UPDATE)
     * @param onDownloadProgress Callback for download progress
     * @param onExtractProgress Callback for extraction progress
     */
    suspend fun downloadChart(
        url: String,
        id: String,
        operation: OperationOption,
        onDownloadProgress: (Float) -> Unit = {},
        onExtractProgress: (Float) -> Unit = {}
    ) {
        val folderUri = StorageUtils.getFolderUri(context, BEATSTAR_URI)
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        // Download the zip file to cache
        val downloadedFile = downloadManager.downloadFileToCache(
            url,
            id,
            "zip",
            onDownloadProgress
        )

        // Notify server about the download (this should not block)
        chartManager.postAnalytics(id, operation)

        // Extract the zip file to the folder
        try {
            val chartFolder = downloadManager.extractZipToFolder(
                downloadedFile,
                id,
                folderUri,
                listOf("songs"),
                onExtractProgress
            )

            // Write the per-chart manifest so the hydration system can identify
            // the chart by its canonical id even after an app reinstall.
            tourPassStorageManager.writeChartIdFile(
                folder = chartFolder,
                id = id
            )
        } catch (e: IOException) {
            throw e
        } finally {
            // Clean up temporary files independently of success
            downloadedFile.delete()
            println("Deleted temporary file: ${downloadedFile.absolutePath}")
        }

        // Update the chart list
        val updateResult = chartManager.updateContentById(id, operation)
        if (updateResult is ContentResult.Error) {
            val msg = when (val m = updateResult.message) {
                is UiText.Plain -> m.value
                is UiText.Res -> context.getString(m.resId, *m.args.toTypedArray())
            }
            throw Exception(msg)
        }
    }

    suspend fun deleteChart(id: String) {
        val destinationFolderUri = StorageUtils.getFolderUri(context, BEATSTAR_URI)
            ?: throw IllegalStateException("Could not access or create beatstar folder")

        try {
            downloadManager.deleteFolderFromUri(
                id,
                destinationFolderUri,
                listOf("songs"),
            )
        } catch (_: NotFoundException) {
            // Folder does not exist, nothing to delete
        }

        // Update the chart list
        val updateResult = chartManager.updateContentById(id, OperationOption.DELETE)
        if (updateResult is ContentResult.Error) {
            val msg = when (val m = updateResult.message) {
                is UiText.Plain -> m.value
                is UiText.Res -> context.getString(m.resId, *m.args.toTypedArray())
            }
            throw Exception(msg)
        }
    }
}