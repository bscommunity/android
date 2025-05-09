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
import kotlinx.serialization.Serializable

@Serializable
object Workshop

@Serializable
enum class UpdatesSection {
	Workshop,
	Installations
}

@Serializable
data class Updates(val section: UpdatesSection)

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

@Composable
fun BottomNavBar(
	selectedItemIndex: Int,
	setItemIndex: (Int) -> Unit,
	navController: NavHostController,
	bottomNavigationItems: List<BottomNavigationItem>
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

						// Restore cacheState when reselecting a previously selected item
						restoreState = true
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