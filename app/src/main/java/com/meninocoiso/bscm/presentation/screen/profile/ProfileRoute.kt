package com.meninocoiso.bscm.presentation.screen.profile

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.RouteUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.viewmodel.PublicProfileViewModel

@Composable
fun ProfileRoute(
    username: String,
    user: SimplifiedUser?,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (SimplifiedCollection) -> Unit,
    publicViewModel: PublicProfileViewModel = hiltViewModel()
) {
    // If user is accessing own profile, return profile screen directly
    if (user != null) {
        RouteUI {
            ProfileScreen(
                user = user,
                onReturn = onReturn,
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToCollection = onNavigateToCollection
            )
        }
    } else {
        val state by publicViewModel.profile.collectAsStateWithLifecycle()

        // Load public profile
        LaunchedEffect(username) {
            publicViewModel.loadProfile(username)
        }

        RouteUI {
            when (state) {
                is ContentResult.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ContentResult.Success -> {
                    val data = (state as ContentResult.Success<UserProfileResponse>).data
                    PublicProfileScreen(
                        user = data.user,
                        isFollowing = data.isFollowing ?: false,
                        onReturn = onReturn,
                        onNavigateToDetails = onNavigateToDetails,
                        onNavigateToCollection = onNavigateToCollection
                    )
                }

                is ContentResult.Error -> {
                    StatusMessageUI(
                        title = "Failed to load profile",
                        message = (state as ContentResult.Error).message,
                        icon = R.drawable.rounded_error_24,
                        onClick = {
                            publicViewModel.loadProfile(username)
                        },
                        buttonLabel = stringResource(R.string.retry),
                    )
                }
            }
        }
    }
}