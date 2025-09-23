package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel

@Composable
fun OfflineLikeButton(
    contentType: ContentType,
    contentId: ULong,
    viewModel: InteractionViewModel = hiltViewModel()
) {
    var isLiked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Observe like status
    val likeStatus by viewModel.getLikeStatus(contentType, contentId).collectAsState(initial = Result.success(false))
    
    LaunchedEffect(likeStatus) {
        likeStatus.onSuccess { liked ->
            isLiked = liked
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }
    
    LikeButton(
        defaultValue = isLiked,
        onLikeChanged = { shouldLike ->
            if (shouldLike != isLiked) {
                if (shouldLike) {
                    viewModel.likeContent(contentType, contentId)
                } else {
                    viewModel.unlikeContent(contentType, contentId)
                }
                isLiked = shouldLike // Optimistic update
            }
        }
    )
}
