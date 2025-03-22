package com.meninocoiso.beatstarcommunity.util

import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.model.ContentMessage

class ContentMessageUtils {
    companion object {
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
    }
}