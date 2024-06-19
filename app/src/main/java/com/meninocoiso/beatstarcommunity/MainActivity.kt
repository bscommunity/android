package com.meninocoiso.beatstarcommunity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meninocoiso.beatstarcommunity.screens.Settings
import com.meninocoiso.beatstarcommunity.screens.Updates
import com.meninocoiso.beatstarcommunity.screens.Workspace
import com.meninocoiso.beatstarcommunity.ui.theme.BeatstarCommunityTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This app draws behind the system bars, so we want to handle fitting system windows
        enableEdgeToEdge()
        setContent {
            BeatstarCommunityTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = {
                            BottomNavigationComponent(navController)
                        }
                    ) { innerPadding ->
                        NavHost(
                            modifier = Modifier.padding(innerPadding),
                            navController = navController,
                            startDestination = WorkspaceScreen,
                        ) {
                            composable<WorkspaceScreen> {
                                Workspace()
                            }
                            composable<UpdatesScreen> {
                                Updates()
                            }
                            composable<SettingsScreen> {
                                Settings()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Serializable
object WorkspaceScreen

@Serializable
object UpdatesScreen

@Serializable
object SettingsScreen

data class BottomNavigationItem(
    val route: Any,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasNews: Boolean,
    val badgeCount: Int? = null
)

val bottomNavigationItems = listOf(
    BottomNavigationItem(
        route = WorkspaceScreen,
        title = "Workspace",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        hasNews = false
    ),
    BottomNavigationItem(
        route = UpdatesScreen,
        title = "Updates",
        selectedIcon = Icons.Filled.ThumbUp,
        unselectedIcon = Icons.Outlined.ThumbUp,
        hasNews = false
    ),
    BottomNavigationItem(
        route = SettingsScreen,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        hasNews = false
    )
)

@Composable
fun BottomNavigationComponent(navController: NavHostController) {
    NavigationBar {
        var selectedItemIndex by rememberSaveable {
            mutableIntStateOf(0)
        }

        bottomNavigationItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = {
                    selectedItemIndex = index
                    navController.navigate(item.route)
                },
                label = {
                    Text(text = item.title)
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
                            imageVector = if (index == selectedItemIndex) {
                                item.selectedIcon
                            } else item.unselectedIcon,
                            contentDescription = item.title
                        )
                    }
                }
            )
        }
    }
}
