package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel

@Composable
fun OfflineLikeButton(
    contentId: String,
    viewModel: InteractionViewModel = hiltViewModel()
) {
    var isLiked by remember { mutableStateOf(false) }
    
    // Observe like status using LaunchedEffect to call suspend function
    LaunchedEffect(contentId) {
        viewModel.getLikeStatus(contentId).collect { result ->
            result.onSuccess { liked ->
                isLiked = liked
            }
        }
    }
    
    LikeButton(
        defaultValue = isLiked,
        onLikeChanged = { shouldLike ->
            if (shouldLike != isLiked) {
                if (shouldLike) {
                    viewModel.likeContent(contentId)
                } else {
                    viewModel.unlikeContent(contentId)
                }
                isLiked = shouldLike // Optimistic update
            }
        }
    )
}
