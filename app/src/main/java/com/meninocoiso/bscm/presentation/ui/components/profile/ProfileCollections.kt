package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.CollectionPreview
import kotlinx.coroutines.launch

@Composable
fun ProfileCollections(
    items: List<Collection>,
    state: ContentState,
    onFetch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tabItems = listOf("All", "Collections", "Charts", "Tour Passes", "Themes")

    val horizontalPagerState = rememberPagerState { tabItems.size }

    val bookmarksCollection = items.find { it.name == "bookmarks" }

    val customCollections = items.filter {
        it.name != "bookmarks" && it.name != "liked"
    }

    if (bookmarksCollection == null) {
        // Show loading or error state if necessary
        return
    }

    println("ProfileCollections: bookmarks=${bookmarksCollection.items.size}, custom=${customCollections.size}")

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxWidth(),
                message = "No collections found",
                icon = R.drawable.rounded_bookmark_24
            )
        }
    ) {
        Column(modifier) {
            CatalogFilters(
                bookmarksCollection.items,
                items.size - 2,
                onFilterSelected = { index ->
                    coroutineScope.launch {
                        // Update pager when a tab is selected
                        horizontalPagerState.animateScrollToPage(index)
                    }
                },
            )

                HorizontalPager(
                    state = horizontalPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(800.dp),
                    key = { it }, // Recompose the pager when the page changes
                    beyondViewportPageCount = 1 // Keep the next page in memory
                ) { index ->
                    when (index) {
                        0 -> {
                            // All
                            ProfileCollectionTabContent(
                                items = bookmarksCollection.items,
                                onFetch = onFetch,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        1 -> {
                            // Collections
                            ProfileCollectionGrid(
                                items = customCollections,
                                onFetch = onFetch,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        2 -> {
                            // Charts
                            ProfileCollectionTabContent(
                                items = bookmarksCollection.items,
                                onFetch = onFetch,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        else -> {
                            // Other tabs (Tour Passes, Themes) - Placeholder
                            StatusMessageUI(
                                modifier = Modifier.fillMaxWidth(),
                                message = "No content available",
                                icon = R.drawable.outline_library_music_24
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun ProfileCollectionTabContent(
    items: List<CatalogItem>,
    onFetch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = ContentState.Success

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxWidth(),
                message = "No favorited content",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyColumn(modifier) {
            contentList(items)
        }
    }
}

@Composable
fun ProfileCollectionGrid(
    items: List<Collection>,
    onFetch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = ContentState.Success

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxWidth(),
                message = "No content in library",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Fixed(2)
        ) {
            items(items.size) { index ->
                val item = items[index]
                CollectionPreview(
                    collection = item,
                    onPress = { /* Navigate to collection details */ }
                )
            }
        }
    }
}