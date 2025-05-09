package com.meninocoiso.beatstarcommunity.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class LinkingUtils {
    companion object {
        fun openLink(context: Context, url: String) {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        }
        
        fun shareChartLink(context: Context, chartId: String) {
            val deepLink = "bscm://chart/details/$chartId"
            // val shareableLink = "https://www.bscm.com/chart/$chartId"

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, deepLink)
                // putExtra(Intent.EXTRA_TEXT, shareableLink)
                type = "text/plain"
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }
}