package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel

@Composable
fun OfflineLikeButton(
    contentId: String,
    defaultValue: Boolean = false,
    viewModel: InteractionViewModel = hiltViewModel()
) {
    var isLiked by remember { mutableStateOf(defaultValue) }
    
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
