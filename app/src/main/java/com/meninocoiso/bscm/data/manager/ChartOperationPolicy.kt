package com.meninocoiso.bscm.data.manager

import android.content.Context
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ContentOperationPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject

class ChartOperationPolicy @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ContentOperationPolicy<Chart> {

    override fun apply(existing: Chart, operation: OperationOption): Result<Chart> {
        return when (operation) {
            OperationOption.INSTALL -> Result.success(existing.copy(isInstalled = true))

            OperationOption.UPDATE -> existing.availableVersion?.let {
                Result.success(
                    existing.copy(
                        latestVersion = it,
                        availableVersion = null
                    )
                )
            }
                ?: Result.failure(IllegalStateException(context.getString(R.string.no_available_version)))

            OperationOption.DELETE -> Result.success(existing.copy(isInstalled = false))

            OperationOption.LIKE -> Result.success(existing.copy(likedAt = LocalDateTime.now()))

            OperationOption.UNLIKE -> Result.success(existing.copy(likedAt = null))

            OperationOption.BOOKMARK -> Result.success(existing.copy(bookmarkedAt = LocalDateTime.now()))

            OperationOption.UNBOOKMARK -> Result.success(existing.copy(bookmarkedAt = null))
        }
    }
}
