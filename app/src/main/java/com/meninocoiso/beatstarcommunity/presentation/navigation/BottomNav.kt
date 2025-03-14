package com.meninocoiso.beatstarcommunity.presentation.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.toRoute
import com.meninocoiso.beatstarcommunity.presentation.screens.ChartDetails
import com.meninocoiso.beatstarcommunity.presentation.screens.SettingsScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.UpdatesScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.WorkspaceScreen
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.LaunchAppButton
import kotlinx.serialization.Serializable

@Serializable
object MainRoute

@Composable
fun BottomNav(
	bottomNavController: NavHostController,
	navController: NavHostController
) {
	var selectedItemIndex by rememberSaveable {
		mutableIntStateOf(0)
	}
	var fabExtended by remember { mutableStateOf(true) }

	Scaffold(
		bottomBar = {
			BottomNavigationComponent(
				selectedItemIndex,
				setItemIndex = { selectedItemIndex = it },
				navController = bottomNavController
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
				extended = fabExtended // Pass the state here
			)
		}
	) { innerPadding ->
		NavHost(
			modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
			navController = bottomNavController,
			startDestination = Workspace,
		) {
			composableWithFade<Workspace> {
				WorkspaceScreen(
					onNavigateToDetails = { chart ->
						println("Navigating to chart details: $chart")
						navController.navigate(route = ChartDetails(
							chart = chart
						))
					},
					onFabStateChange = { shouldExtend ->
						if (shouldExtend != fabExtended) {
							Log.d("BottomNav", "Fab state changed: $shouldExtend")
							fabExtended = shouldExtend
						}
					}
				)
			}
			composableWithFade<Updates> { backStackEntry ->
				val updates: Updates = backStackEntry.toRoute()
				UpdatesScreen(
					updates.section,
					onFabStateChange = { shouldExtend ->
						if (shouldExtend != fabExtended) {
							fabExtended = shouldExtend
						}
					}
				)
			}
			composableWithFade<Settings> {
				SettingsScreen(
					onFabStateChange = { shouldExtend ->
						if (shouldExtend != fabExtended) {
							fabExtended = shouldExtend
						}
					}
				)
			}
		}
	}
}