package com.jarvis.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jarvis.ui.components.JarvisBackground
import com.jarvis.ui.screens.ActivityScreen
import com.jarvis.ui.screens.ChatScreen
import com.jarvis.ui.screens.DiagnosticsScreen
import com.jarvis.ui.screens.HomeScreen
import com.jarvis.ui.screens.MemoryScreen
import com.jarvis.ui.screens.ModelSettingsScreen
import com.jarvis.ui.screens.PermissionsScreen
import com.jarvis.ui.screens.PrivacyScreen
import com.jarvis.ui.screens.SettingsScreen
import com.jarvis.ui.screens.VoiceScreen

/** Type-safe routes. */
object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val VOICE = "voice"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"
    const val MODEL_SETTINGS = "model_settings"
    const val PRIVACY = "privacy"
    const val PERMISSIONS = "permissions"
    const val ACTIVITY = "activity"
    const val DIAGNOSTICS = "diagnostics"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.CHAT, "Chat", Icons.Filled.Chat),
    TopLevelDestination(Routes.VOICE, "Voice", Icons.Filled.Mic),
    TopLevelDestination(Routes.MEMORY, "Memory", Icons.Filled.Memory),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun JarvisNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        JarvisBackground {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenChat = { navController.navigate(Routes.CHAT) },
                        onOpenVoice = { navController.navigate(Routes.VOICE) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }
                composable(Routes.CHAT) { ChatScreen() }
                composable(Routes.VOICE) { VoiceScreen() }
                composable(Routes.MEMORY) { MemoryScreen() }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onOpenModelSettings = { navController.navigate(Routes.MODEL_SETTINGS) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                        onOpenActivity = { navController.navigate(Routes.ACTIVITY) },
                        onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
                    )
                }
                composable(Routes.MODEL_SETTINGS) { ModelSettingsScreen() }
                composable(Routes.PRIVACY) { PrivacyScreen() }
                composable(Routes.PERMISSIONS) { PermissionsScreen() }
                composable(Routes.ACTIVITY) { ActivityScreen() }
                composable(Routes.DIAGNOSTICS) { DiagnosticsScreen() }
            }
        }
    }
}
