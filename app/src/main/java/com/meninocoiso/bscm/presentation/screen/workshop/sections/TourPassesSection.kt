package com.meninocoiso.bscm.presentation.screen.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.meninocoiso.bscm.domain.model.TourPass
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
            contributors = listOf()
        )
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        SectionWrapper(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .fabScrollObserver { shouldExtend ->
                    // Update FAB state based on scroll delta
                    onFabStateChange(shouldExtend)
                },
            listState = listState,
        ) {
            // Add WorkshopChips as the first item in the list
            /*item {
                WorkshopChips(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .zIndex(1f), // Lower z-index since it's now part of the scrollable content
                    currentSortOption = viewModel.currentSortOption,
                    onSortOptionChange = viewModel::changeSortOption
                )
            }*/

            itemsIndexed(tourPasses) { index, tourPass ->
                TourPassPreview(
                    tourPass = tourPass,
                    isDisabled = false,
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