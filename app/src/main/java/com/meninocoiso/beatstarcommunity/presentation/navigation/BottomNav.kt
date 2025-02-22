package com.meninocoiso.beatstarcommunity.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.toRoute
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.screens.ChartDetails
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.LaunchAppButton
import com.meninocoiso.beatstarcommunity.presentation.screens.SettingsScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.UpdatesScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.WorkspaceScreen
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
				}
			)
		}
	) { innerPadding ->
		NavHost(
			modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
			navController = bottomNavController,
			startDestination = Workspace,
		) {
			composableWithFade<Workspace> {
				WorkspaceScreen(onNavigateToDetails = { chart ->
					println("Navigating to chart details: $chart")
					navController.navigate(route = ChartDetails(
						chart = chart
					))
				})
			}
			composableWithFade<Updates> { backStackEntry ->
				val updates: Updates = backStackEntry.toRoute()
				UpdatesScreen(updates.section)
			}
			composableWithFade<Settings> {
				SettingsScreen()
			}
		}
	}
}