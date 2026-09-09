package com.meninocoiso.bscm.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.meninocoiso.bscm.R

object LinkingUtils {
    fun openLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    private fun shareLink(context: Context, url: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }

        context.startActivity(Intent.createChooser(shareIntent,
            context.getString(R.string.share_via)))
    }

    fun shareChart(context: Context, id: String) {
        val shareableLink = "https://bscm.netlify.app/link/chart/$id"
        shareLink(context, shareableLink)
    }

    fun shareTourPass(context: Context, id: String) {
        val shareableLink = "https://bscm.netlify.app/link/tourpass/$id"
        shareLink(context, shareableLink)
    }

    fun shareProfile(context: Context, username: String) {
        val shareableLink = "https://bscm.netlify.app/link/profile/$username"
        shareLink(context, shareableLink)
    }

    fun shareCollection(context: Context, username: String, slug: String?) {
        val shareableLink = "https://bscm.netlify.app/link/collection/$username/$slug"
        shareLink(context, shareableLink)
    }

    fun launchGame(
        context: Context,
        openAlertDialog: (Boolean) -> Unit,
    ) {
        val packageNames = listOf("com.spaceapegames.beatstar", "com.spaceapegames.beatclon")
        var launchIntent: Intent? = null

        for (packageName in packageNames) {
            launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) break
        }

        println("launchIntent: $launchIntent")

        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            openAlertDialog(true)
        }
    }
}