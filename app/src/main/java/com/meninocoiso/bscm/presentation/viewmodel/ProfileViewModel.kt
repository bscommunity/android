package com.meninocoiso.bscm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Role
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.result.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ProfileViewModel"

data class ActivityItem(val date: Date, val content: List<CatalogItem>)

val placeholderChart = Chart(
    id = "placeholder_id",
    contentId = "share_placeholder",
    artist = "Artista Fictício",
    track = "Música Exemplo",
    album = "Álbum Exemplo",
    genre = null,
    coverUrl = "",
    trackUrls = emptyList(),
    trackPreviewUrl = "",
    isFeatured = false,
    isInstalled = false,
    downloadsSum = 0,
    latestPublishedAt = LocalDateTime.now(),
    latestVersion = Version(
        id = 1L,
        chartId = "placeholder_chart_id",
        index = 1,
        duration = 180f,
        notesAmount = 1000,
        effectsAmount = 50,
        bpm = 128,
        difficulty = Difficulty.NORMAL,
        isDeluxe = false,
        isExplicit = false,
        bundleUrl = "",
        previewUrl = null,
        downloadsAmount = 0,
        knownIssues = emptyList(),
        publishedAt = LocalDateTime.now()
    ),
    availableVersion = null,
    contributors = listOf(
        Contributor(
            user = SimplifiedUser(
                id = "user_placeholder_id",
                username = "ContribuidorExemplo",
                avatarUrl = null,
            ),
            chartId = "placeholder_chart_id",
            roles = listOf(Role.AUDIO),
            joinedAt = LocalDateTime.now()
        ),
        Contributor(
            user = SimplifiedUser(
                id = "user_placeholder_id",
                username = "meumano2",
                avatarUrl = null,
            ),
            chartId = "placeholder_chart_id",
            roles = listOf(Role.AUDIO),
            joinedAt = LocalDateTime.now()
        ),
        Contributor(
            user = SimplifiedUser(
                id = "user_placeholder_id",
                username = "ala3alalalala3",
                avatarUrl = null,
            ),
            chartId = "placeholder_chart_id",
            roles = listOf(Role.AUDIO),
            joinedAt = LocalDateTime.now()
        )
    ),
)

val placeholderActivityItems = listOf(
    ActivityItem(
        date = Date(),
        content = listOf(placeholderChart, placeholderChart, placeholderChart)
    ),
    ActivityItem(
        date = Date(),
        content = listOf(placeholderChart, placeholderChart)
    ),
    ActivityItem(
        date = Date(),
        content = listOf(placeholderChart, placeholderChart, placeholderChart)
    ),
    ActivityItem(
        date = Date(),
        content = listOf(placeholderChart, placeholderChart, placeholderChart)
    )
)

val placeholderLibraryItems = listOf<CatalogItem>(
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
)

val placeholderCollection = Collection(
    id = ULong.MIN_VALUE,
    name = "Minha Coleção Exemplo",
    coverUrl = "https://i.imgur.com/sDP3mcd.jpeg",
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    userId = UUID.randomUUID(),
    isPublic = true,
    items = listOf(
        placeholderChart,
        placeholderChart,
        placeholderChart,
        placeholderChart,
        placeholderChart,
        placeholderChart,
        placeholderChart,
        placeholderChart
    )
)

val favoriteCollection = Collection(
    id = ULong.MAX_VALUE - 1u,
    name = "bookmarks",
    coverUrl = "https://i.imgur.com/sDP3mcd.jpeg",
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    userId = UUID.randomUUID(),
    isPublic = false,
    items = listOf(placeholderChart, placeholderChart)
)

val placeholderCollections = listOf(
    favoriteCollection,
    placeholderCollection,
    placeholderCollection,
    placeholderCollection
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    /*@param:Named("Remote") private val remoteChartRepository: ChartRepository,
    @param:Named("Local") private val localChartRepository: ChartRepository,*/
) : ViewModel() {
    // Activity = all last 25 content interactions (likes, comments, new follows, new content from followed users)
    // Library = all content created by the user, sorted by upload date
    // Likes = all content liked by the user, sorted by like date
    // Collections = all content added to collections by the user, sorted by addition date

    private val _section1State = MutableStateFlow<ContentState>(ContentState.Loading)
    val section1State: SharedFlow<ContentState> = _section1State.asStateFlow()
    private val _section2State = MutableStateFlow<ContentState>(ContentState.Loading)
    val section2State: SharedFlow<ContentState> = _section2State.asStateFlow()

    // Public info
    private val _activityContent = MutableStateFlow<List<ActivityItem>>(placeholderActivityItems)
    val activityContent = MutableStateFlow<List<ActivityItem>>(placeholderActivityItems)

    private val _libraryContent = MutableStateFlow<List<CatalogItem>>(placeholderLibraryItems)
    val libraryContent = MutableStateFlow<List<CatalogItem>>(placeholderLibraryItems)

    // Private info (logged-in user only)
    private val _likedContent = MutableStateFlow<List<CatalogItem>>(emptyList())
    val likedContent: StateFlow<List<CatalogItem>> = _likedContent.asStateFlow()

    private val _collectionContent = MutableStateFlow<List<Collection>>(placeholderCollections)
    val collectionContent = MutableStateFlow<List<Collection>>(placeholderCollections)

    fun loadUserProfile(userId: String) {
        // Implementation for loading profile data
    }

    fun fetchProfileActivity(userId: String) {
        // Implementation for fetching profile activity
    }

    fun fetchProfileLibrary(userId: String) {
        // Implementation for fetching profile library
    }

    fun fetchUserLikes() {
        // Implementation for fetching user likes
    }

    // If likes were not fetched yet, fetch them
    fun fetchUserCollections() {
        // Implementation for fetching user collections
    }
}