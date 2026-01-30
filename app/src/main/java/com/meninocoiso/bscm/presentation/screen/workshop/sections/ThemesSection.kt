package com.meninocoiso.bscm.presentation.screen.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.enums.Role
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.layout.GridSectionWrapper
import com.meninocoiso.bscm.presentation.ui.components.preview.ThemePreview
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemesSection(
    onFabStateChange: (Boolean) -> Unit,
) {
    val themes = listOf(
        // Sample TourPass data
        Theme(
            id = "1",
            name = "Bassline Yatteru (aka. Can I Friend You On Bassbook ? Lol)",
            replaces = "Rock - Chrome Skull",
            contentId = "1234567890",
            coverUrl = "https://i.imgur.com/g23iXuT.png",
            previewUrl = "https://i.imgur.com/lTPUHIN.png",
            isFeatured = false,
            latestPublishedAt = LocalDateTime.now(),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(id = "1", username = "meninocoiso", imageUrl = "https://i.imgur.com/5Hsj4tJ.jpeg"),
                    chartId = "asdads",
                    roles = listOf(Role.GAMEPLAY),
                    joinedAt = LocalDateTime.now()
                )
            )
        ),
        Theme(
            id = "1",
            name = "Daft Punk",
            replaces = "Dance - Fastlane",
            contentId = "1234567890",
            coverUrl = "https://i.imgur.com/yGZjCNv.png",
            previewUrl = "https://i.imgur.com/ux3WDfi.png",
            isFeatured = false,
            latestPublishedAt = LocalDateTime.now(),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(id = "1", username = "meninocoiso", imageUrl = "https://i.imgur.com/5Hsj4tJ.jpeg"),
                    chartId = "asdads",
                    roles = listOf(Role.GAMEPLAY),
                    joinedAt = LocalDateTime.now()
                )
            )
        ),
        Theme(
            id = "1",
            name = "The Cyber Grind",
            replaces = "Rock - Chrome Skull",
            contentId = "1234567890",
            coverUrl = "https://i.imgur.com/7XsJ6GC.png",
            previewUrl = "https://i.imgur.com/KaiDZBH.png",
            isFeatured = false,
            latestPublishedAt = LocalDateTime.now(),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(id = "1", username = "meninocoiso", imageUrl = "https://i.imgur.com/5Hsj4tJ.jpeg"),
                    chartId = "asdads",
                    roles = listOf(Role.GAMEPLAY),
                    joinedAt = LocalDateTime.now()
                )
            )
        ),
        Theme(
            id = "1",
            name = "Green V1",
            replaces = "Rock - Chrome Skull",
            contentId = "1234567890",
            coverUrl = "https://i.imgur.com/QYpcMfh.png",
            previewUrl = "https://i.imgur.com/7fs2XWg.png",
            isFeatured = false,
            latestPublishedAt = LocalDateTime.now(),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(id = "1", username = "meninocoiso", imageUrl = "https://i.imgur.com/5Hsj4tJ.jpeg"),
                    chartId = "asdads",
                    roles = listOf(Role.GAMEPLAY),
                    joinedAt = LocalDateTime.now()
                )
            )
        ),
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        StatusMessageUI(
            modifier = Modifier.zIndex(50f).fillMaxSize(),
            title = stringResource(R.string.work_in_progress),
            message = stringResource(R.string.work_in_progress_description),
            icon = R.drawable.rounded_hourglass_24
        )
        GridSectionWrapper(
            modifier = Modifier
                .fillMaxSize()
                // .nestedScroll(nestedScrollConnection)
                .fabScrollObserver { shouldExtend ->
                    // Update FAB state based on scroll delta
                    onFabStateChange(shouldExtend)
                }
                .graphicsLayer {
                    alpha = 0.35f
                }
            ,
            // listState = listState,
        ) {
            // Add WorkshopChips as the first item in the list
            /*item {
                WorkshopChips(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .zIndex(1f), // Lower z-index since it's now part of the scrollable content
                    currentSortOption = SortOption.MOST_DOWNLOADED,
                    onSortOptionChange = { }
                )
            }*/

            items(themes) { theme ->
                ThemePreview(
                    theme = theme,
                    isDisabled = true,
                    onNavigateToDetails = {
                        // onNavigateToDetails(chart)
                    },
                )
            }
        }
    }
}