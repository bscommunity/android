package com.meninocoiso.bscm.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

object LinkingUtils {
    fun openLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    fun shareChartLink(context: Context, chartId: String) {
        // val deepLink = "bscm://chart/details/$chartId"
        val shareableLink = "https://bscm.netlify.app/link/chart/$chartId"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareableLink)
            type = "text/plain"
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

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