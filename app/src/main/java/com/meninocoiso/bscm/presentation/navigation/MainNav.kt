package com.meninocoiso.bscm.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.presentation.screen.details.ChartDetailsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
object MainRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNav(
    startOAuth: (Uri) -> Unit,
    user: SimplifiedUser?,
    hasUpdate: Boolean,
    intentFlow: Flow<Intent>
) {
    val navController = rememberNavController()
    val bottomNavController = rememberNavController()

    // Wrap the SharedTransitionLayout with a Box that paints the background to avoid white flashes
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = MainRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Collection screen
                composable<MainRoute> { backStackEntry ->
                    ChartDetailsScreen( chart = Chart(
                        artist = "Placeholder Artist",
                        track = "Placeholder Track",
                        album = null,
                        genre = null,
                        colors = null,
                        trackUrls = emptyList(),
                        trackPreviewUrl = null,
                        id = "placeholder_id",
                        contentId = null,
                        coverUrl = "https://placeholder.com/cover",
                        isFeatured = false,
                        downloadsSum = 0,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                        likedAt = null,
                        bookmarkedAt = null,
                        contributors = emptyList(),
                        isInstalled = false,
                        latestVersion = Version(
                            id = 0L,
                            chartId = "placeholder",
                            index = 0,
                            duration = 0f,
                            notesAmount = 0,
                            effectsAmount = 0,
                            bpm = 120,
                            difficulty = Difficulty.EXPERT,
                            isDeluxe = false,
                            isExplicit = false,
                            bundleUrl = "https://placeholder.com/bundle",
                            previewUrl = null,
                            downloadsAmount = 0,
                            knownIssues = emptyList(),
                            createdAt = LocalDateTime.now()
                        ),
                        availableVersion = null
                    ),
                        onReturn = {},
                        onNavigateToSettings = {})
                }
            }
        }
    }
}