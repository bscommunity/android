package com.meninocoiso.bscm.presentation.screen.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.enums.Role
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.bscm.presentation.ui.components.preview.TourPassPreview
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TourPassesSection(
    nestedScrollConnection: NestedScrollConnection,
    listState: LazyListState,
    onFabStateChange: (Boolean) -> Unit,
) {
    val tourPasses = listOf(
        // Sample TourPass data
        TourPass(
            id = "1",
            name = "The World's a Little Blurry",
            artist = "Billie Eilish",
            contentId = "1234567890",
            coverUrl = "https://i.imgur.com/WsewcFR.jpeg",
            isFeatured = false,
            latestPublishedAt = LocalDateTime.now(),
            charts = listOf(),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(id = "1", username = "meninocoiso", imageUrl = "https://i.imgur.com/5Hsj4tJ.jpeg"),
                    chartId = "asdads",
                    roles = listOf(Role.GAMEPLAY),
                    joinedAt = LocalDateTime.now()
                )
            )
        ),
        TourPass(
            id = "2",
            name = "This is what _______ feels like",
            artist = "JVKE",
            contentId = "123456734890",
            coverUrl = "https://i.imgur.com/jeGiroM.png",
            isFeatured = false,
            latestPublishedAt = LocalDateTime.now(),
            charts = listOf(),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(id = "1", username = "meninocoiso", imageUrl = "https://i.imgur.com/5Hsj4tJ.jpeg"),
                    chartId = "asdads",
                    roles = listOf(Role.GAMEPLAY),
                    joinedAt = LocalDateTime.now()
                )
            )
        ),
        TourPass(
            id = "3",
            name = "The Clancy Experience",
            artist = "Twenty One Pilots",
            contentId = "123877434890",
            coverUrl = "https://i.imgur.com/HcmI0fW.jpeg",
            isFeatured = false,
            latestPublishedAt = LocalDateTime.now(),
            charts = listOf(),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(id = "1", username = "meninocoiso", imageUrl = "https://i.imgur.com/5Hsj4tJ.jpeg"),
                    chartId = "asdads",
                    roles = listOf(Role.GAMEPLAY),
                    joinedAt = LocalDateTime.now()
                )
            )
        )
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        StatusMessageUI(
            title = stringResource(R.string.work_in_progress),
            message = stringResource(R.string.work_in_progress_description),
            icon = R.drawable.rounded_hourglass_24,
            modifier = Modifier.zIndex(50f).fillMaxSize()
        )
        SectionWrapper(
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
            listState = listState,
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

            itemsIndexed(tourPasses) { index, tourPass ->
                TourPassPreview(
                    tourPass = tourPass,
                    isDisabled = true,
                    onNavigateToDetails = {
                        // onNavigateToDetails(chart)
                    },
                )
            }

            /*if (viewModel.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.workshop_feed_end),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }*/
        }
    }
}