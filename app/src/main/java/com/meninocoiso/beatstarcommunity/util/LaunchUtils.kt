package com.meninocoiso.beatstarcommunity.util

import android.content.Context

class LaunchUtils {
    companion object {
        fun launchBeatClone(
            context: Context,
            openAlertDialog: (Boolean) -> Unit,
        ) {
            val packageName = "com.spaceapegames.beatclon"

            // Create an Intent to launch the app
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

            println("launchIntent: $launchIntent")

            if (launchIntent != null) {
                // If the intent is not null, start the activity
                context.startActivity(launchIntent)
            } else {
                // If the app is not installed,
                openAlertDialog(true)
            }
        }
    }
}