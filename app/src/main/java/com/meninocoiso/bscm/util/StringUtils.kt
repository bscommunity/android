package com.meninocoiso.bscm.util

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.model.internal.ContentMessage
import java.time.LocalDate
import java.time.ZoneId

object StringUtils {
    // Download state messages
    fun getInitialMessage(
        chartName: String,
        type: OperationType
    ): ContentMessage {
        return when (type) {
            OperationType.UPDATE -> {
                ContentMessage(
                    title = "Updating $chartName",
                    message = "Checking for updates for $chartName..."
                )
            }

            else -> {
                ContentMessage(
                    title = "Downloading $chartName",
                    message = "Starting download..."
                )
            }
        }
    }

    fun getProgressMessage(
        chartName: String,
        progress: Int,
        type: OperationType
    ): ContentMessage {
        return when (type) {
            OperationType.UPDATE -> {
                ContentMessage(
                    title = "Updating $chartName",
                    message = "Updating $progress%..."
                )
            }

            else -> {
                ContentMessage(
                    title = "Downloading $chartName",
                    message = "Downloading... $progress%..."
                )
            }
        }
    }

    fun getFinalMessage(
        chartName: String,
        type: OperationType
    ): ContentMessage {
        return when (type) {
            OperationType.UPDATE -> {
                ContentMessage(
                    title = "Updated $chartName",
                    message = "Update completed!"
                )
            }

            else -> {
                ContentMessage(
                    title = "Download complete",
                    message = "$chartName has been downloaded successfully"
                )
            }
        }
    }
    
    fun toRelativeString(date: LocalDate): String {
        val now = LocalDate.now()
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nowMillis = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val relative = DateUtils.getRelativeTimeSpanString(
            dateMillis,
            nowMillis,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE
        ).toString()
        return relative.replaceFirstChar { it.lowercase() }
    }

    @Composable
    fun toDurationString(seconds: Float): String {
        val minutes = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        val context = LocalContext.current
        val measureFormat = MeasureFormat.getInstance(
            context.resources.configuration.locales[0],
            MeasureFormat.FormatWidth.SHORT
        )
        val measures = listOf(
            Measure(minutes, MeasureUnit.MINUTE),
            Measure(secs, MeasureUnit.SECOND)
        )
        return measureFormat.formatMeasures(*measures.toTypedArray())
    }
}