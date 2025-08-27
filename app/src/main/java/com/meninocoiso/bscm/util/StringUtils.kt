package com.meninocoiso.bscm.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.model.internal.ContentMessage
import java.time.Duration
import java.time.LocalDateTime

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
    
    @Composable
    fun toRelativeString(date: LocalDateTime): String {
        val now = LocalDateTime.now()
        val diff = Duration.between(date, now).toDays()

        if (diff < 1) { // today
            return stringResource(R.string.today)
        }

        if (diff < 2) { // yesterday
            return stringResource(R.string.yesterday)
        }

        // days
        if (diff < 7) {
            return pluralStringResource(R.plurals.days_ago, diff.toInt(), diff.toInt())
        }

        // weeks
        if (diff < 30) {
            val weeks = (diff / 7).toInt()
            return pluralStringResource(R.plurals.weeks_ago, weeks, weeks)
        }

        // months
        if (diff < 365) {
            val months = (diff / 30).toInt()
            return pluralStringResource(R.plurals.months_ago, months, months)
        }

        // years
        val years = (diff / 365).toInt()
        return pluralStringResource(R.plurals.years_ago, years, years)
    }

    @Composable
    fun toDurationString( seconds: Float): String {
        val minutes = (seconds % 3600) / 60
        val formattedSeconds = seconds % 60

        return stringResource(
            R.string.minutes_seconds,
            minutes.toInt(),
            formattedSeconds.toInt()
        )
    }
}