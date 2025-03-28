package com.meninocoiso.beatstarcommunity.presentation.ui.components.workshop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.ConfirmationDialog
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

/* TODO: For optional future improvement, Compose devs use the following strategy to detect touch
@Composable
private fun DetectClickFromInteractionSource(
    interactionSource: InteractionSource,
    onClick: () -> Unit,
) {
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) onClick()
        }
    }
}
DetectClickFromInteractionSource(interactionSource) {
            if (!searchBarState.isExpanded) {
                coroutineScope.launch { searchBarState.animateToExpanded() }
            }
        }
* */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun WorkshopSearchBar(
    textFieldState: TextFieldState,
    historyItems: List<String>,
    suggestions: List<String>?,
    onHistoryItemDelete: (String) -> Unit,
    onSearch: (query: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    val haptics = LocalHapticFeedback.current
    var historyItemToDelete by remember { mutableStateOf<String?>(null) }

    /*var isFilterSheetOpen by rememberSaveable {
		mutableStateOf(false)
	}

	val filtersList = remember {
		mutableStateListOf<String>()
	}*/

    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Expanded) {
            textFieldState.setTextAndSelectAll(textFieldState.text.toString())
        }
    }
    
    val onQueryFinish: (String) -> Unit = { query ->
        textFieldState.setTextAndPlaceCursorAtEnd(query)
        onSearch(query)
        scope.launch { searchBarState.animateToCollapsed() }
    }

    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = onQueryFinish,
                placeholder = { Text("Search in workshop") },
                leadingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded ||
                        !textFieldState.text.isEmpty()) {
                        IconButton(onClick = {
                            scope.launch { searchBarState.animateToCollapsed() }
                            textFieldState.setTextAndPlaceCursorAtEnd("")
                            onSearch("")
                        }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                /*trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },*/
            )
        }

    SearchBar(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        state = searchBarState,
        inputField = inputField,
    )
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
        ) {
            if (suggestions != null) {
                suggestions.forEach { it ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { onQueryFinish(it) })
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = it,
                        )
                    }
                }
            } else if (historyItems.isNotEmpty()) {
                Text(
                    text = "Recent searches",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = 8.dp
                    )
                )
                historyItems.forEach { it ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onQueryFinish(it) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    historyItemToDelete = it
                                },
                                onLongClickLabel = "Delete history item"
                            )
                            .padding(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_history_24),
                            contentDescription = "History icon",
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = it,
                        )
                    }
                }
            }
        }
    }
    if (historyItemToDelete != null) {
        ConfirmationDialog(
            onDismiss = { historyItemToDelete = null },
            onConfirm = {
                onHistoryItemDelete(historyItemToDelete!!)
                historyItemToDelete = null
            },
            title = "Remove ${historyItemToDelete!!}",
            message = "Are you sure you want to remove this item from your search history?"
        )
    }
    /*if (isFilterSheetOpen) {
		WorkshopFilterBottomSheet(
			filtersList = filtersList,
			sheetState = filterSheetState,
			onDismissRequest = {
				isFilterSheetOpen = false
			},
			onClose = {
				/*scope.launch { filterSheetState.hide() }.invokeOnCompletion {
					if (!filterSheetState.isVisible) {
						isFilterSheetOpen = false
					}
				}*/
			})
	}*/
}