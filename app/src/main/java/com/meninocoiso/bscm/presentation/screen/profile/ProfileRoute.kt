package com.meninocoiso.bscm.presentation.screen.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileRoute(
    username: String?,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (collectionId: String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.profile.collectAsStateWithLifecycle()
    val isOwnerProfile by viewModel.isOwner.collectAsStateWithLifecycle()

    // If we don't have a profile from typed navigation, fetch it using username
    LaunchedEffect(username) {
        viewModel.loadProfile(username)
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { it
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is ContentResult.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ContentResult.Success -> {
                    val data = (state as ContentResult.Success<UserProfileResponse>).data
                    ProfileScreen(
                        user = data.user,
                        isFollowing = data.isFollowing ?: false,
                        isOwner = isOwnerProfile,
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
                            viewModel.loadProfile(username)
                        },
                        buttonLabel = stringResource(R.string.retry),
                    )
                }
            }
        }
    }
}