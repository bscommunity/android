package com.meninocoiso.beatstarcommunity.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.toRoute
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.screens.SettingsScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.details.ChartDetails
import com.meninocoiso.beatstarcommunity.presentation.screens.updates.UpdatesScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.workshop.WorkshopScreen
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.LaunchAppButton
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object MainRoute

val bottomNavigationItems = listOf(
	BottomNavigationItem(
		route = Workshop,
		title = "Workshop",
		selectedIcon = R.drawable.baseline_library_music_24,
		unselectedIcon = R.drawable.outline_library_music_24,
		hasNews = false
	),
	BottomNavigationItem(
		route = Updates(section = UpdatesSection.Workshop),
		title = "Updates",
		selectedIcon = R.drawable.baseline_deployed_code_24,
		unselectedIcon = R.drawable.outline_deployed_code_24,
		hasNews = false
	),
	BottomNavigationItem(
		route = Settings,
		title = "Settings",
		selectedIcon = R.drawable.baseline_settings_24,
		unselectedIcon = R.drawable.outline_settings_24,
		hasNews = false
	)
)

@Composable
fun BottomNav(
	bottomNavController: NavHostController,
	navController: NavHostController,
	hasUpdate: Boolean = false
) {
	var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

	// We update Settings item icon based on the update status
	val updatedBottomNavigationItems = remember(hasUpdate) {
		bottomNavigationItems.map { item ->
			if (item.route is Settings) {
				item.copy(hasNews = hasUpdate) // Update the badge dynamically
			} else {
				item
			}
		}
	}

	val snackbarHostState = remember { SnackbarHostState() }
	val coroutineScope = rememberCoroutineScope()

	var fabExtended by remember { mutableStateOf(true) }
	// var fabOffset by remember { mutableFloatStateOf(0f) }

	/*LaunchedEffect(snackbarHostState) {
		snapshotFlow { snackbarHostState.currentSnackbarData }
			.collect { snackbarData ->
				fabOffset = if (snackbarData != null) -250f else 0f
			}
	}*/

	val onSnackbar: (String) -> Unit = { message ->
		coroutineScope.launch {
			snackbarHostState.showSnackbar(message)
		}
	}

	val onNavigateToDetails = { chart: Chart ->
		navController.navigate(route = ChartDetails(chart = chart))
	}

	val onFabStateChange: (Boolean) -> Unit = { shouldExtend ->
		if (shouldExtend != fabExtended) {
			fabExtended = shouldExtend
		}
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		bottomBar = {
			BottomNavBar(
				selectedItemIndex,
				setItemIndex = { selectedItemIndex = it },
				navController = bottomNavController,
				bottomNavigationItems = updatedBottomNavigationItems,
			)
		},
		floatingActionButton = {
			LaunchAppButton(
				onNavigateToUpdates = {
					bottomNavController.navigate(
						route = Updates(section = UpdatesSection.Installations)
					)
					selectedItemIndex = 1
				},
				extended = fabExtended,
				/*modifier = Modifier.graphicsLayer {
					translationY = fabOffset
				}*/
			)
		}
	) { innerPadding ->
		NavHost(
			modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
			navController = bottomNavController,
			startDestination = Workshop,
		) {
			composableWithFade<Workshop> {
				WorkshopScreen(
					onNavigateToDetails,
					onFabStateChange,
					onSnackbar
				)
			}
			composableWithFade<Updates> { backStackEntry ->
				val updates: Updates = backStackEntry.toRoute()
				UpdatesScreen(
					updates.section,
					onNavigateToDetails,
					onSnackbar,
					onFabStateChange
				)
			}
			composableWithFade<Settings> {
				SettingsScreen(onFabStateChange, onSnackbar)
			}
		}
	}
}