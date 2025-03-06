package com.meninocoiso.beatstarcommunity.presentation.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import com.meninocoiso.beatstarcommunity.R
import kotlinx.serialization.Serializable

@Serializable
object Workspace

enum class UpdatesSection {
	Workshop,
	Installations
}

@Serializable
data class Updates(val section: UpdatesSection? = null)

@Serializable
object Settings

data class BottomNavigationItem(
	val route: Any,
	val title: String,
	val selectedIcon: Int,
	val unselectedIcon: Int,
	val hasNews: Boolean,
	val badgeCount: Int? = null
)

val bottomNavigationItems = listOf(
	BottomNavigationItem(
		route = Workspace,
		title = "Workspace",
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
fun BottomNavigationComponent(
	selectedItemIndex: Int,
	setItemIndex: (Int) -> Unit,
	navController: NavHostController,
) {
	NavigationBar {
		bottomNavigationItems.forEachIndexed { index, item ->
			NavigationBarItem(
				selected = selectedItemIndex == index,
				onClick = {
					// Prevent navigating to the same destination
					if (selectedItemIndex == index) return@NavigationBarItem

					setItemIndex(index)

					navController.navigate(item.route) {
						// Pop up to the start destination of the graph to
						// avoid building up a large stack of destinations
						// on the back stack as users select items
						popUpTo(navController.graph.startDestinationId) {
							saveState = true
						}

						// Avoid multiple copies of the same destination when
						// reselecting the same item
						launchSingleTop = true

						// Restore state when reselecting a previously selected item
						// restoreState = true
					}
				},
				label = {
					Text(text = item.title, textAlign = TextAlign.Center)
				},
				alwaysShowLabel = true,
				icon = {
					BadgedBox(
						badge = {
							if (item.badgeCount != null) {
								Badge {
									Text(text = item.badgeCount.toString())
								}
							} else if (item.hasNews) {
								Badge()
							}
						}
					) {
						Icon(
							painter = if (index == selectedItemIndex) {
								painterResource(id = item.selectedIcon)
							} else painterResource(id = item.unselectedIcon),
							contentDescription = item.title
						)
					}
				}
			)
		}
	}
}