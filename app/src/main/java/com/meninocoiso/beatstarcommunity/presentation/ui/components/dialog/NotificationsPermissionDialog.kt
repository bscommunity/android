package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun NotificationsPermissionDialog() {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Check if we have notification permission
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // No runtime permission needed before Android 13
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Permission denied
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)
            ) {
                // User checked "Don't ask again" - direct to settings
                showSettings = true
            } else {
                // Show in-app rationale dialog
                showRationale = true
            }
        }
    }

    // Request the permission on first composition if not granted.
    if (!hasNotificationPermission) {
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Rationale dialog (when user denies permission)
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Notifications Required") },
            text = { Text("Notifications are important to show you the status of your downloads.\nWould you like to enable them?") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Settings dialog (when user selected "Don't ask again")
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Enable Notifications") },
            text = { Text("To enable notifications, please open app settings and grant notification permission.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettings = false
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Not Now")
                }
            }
        )
    }
}

// Helper function to check if we should show rationale
fun shouldShowRequestPermissionRationale(context: android.content.Context, permission: String): Boolean {
    return if (context is androidx.activity.ComponentActivity) {
        context.shouldShowRequestPermissionRationale(permission)
    } else {
        false
    }
}