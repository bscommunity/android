package com.meninocoiso.bscm.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    enableSwipeLeft: Boolean = true,
    enableSwipeRight: Boolean = true
) {
    // State for swipe-to-dismiss
    val swipeState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier) {
        // Only show the snackbar content when there is a snackbar
        hostState.currentSnackbarData?.let { snackbarData ->
            SwipeToDismissBox(
                state = swipeState,
                // background behind the snackbar, shown during the swipe
                backgroundContent = {},
                // control which sides are allowed
                enableDismissFromStartToEnd = enableSwipeRight,
                enableDismissFromEndToStart = enableSwipeLeft,
                onDismiss = { direction ->
                    if ((direction == SwipeToDismissBoxValue.StartToEnd && enableSwipeRight) ||
                        (direction == SwipeToDismissBoxValue.EndToStart && enableSwipeLeft)
                    ) {
                        // Dismiss the snackbar
                        hostState.currentSnackbarData?.dismiss()
                        scope.launch {
                            swipeState.reset()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                SnackbarHost(
                    hostState = hostState,
                    modifier = Modifier.fillMaxWidth()
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
