package com.meninocoiso.bscm.presentation.activity

import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback

class OAuthCallback : CustomTabsCallback() {
    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
        when (navigationEvent) {
            NAVIGATION_STARTED -> {
                // URL loading started
                println("CustomTabs - Navigation Started")
            }
            NAVIGATION_FINISHED -> {
                // URL loading finished successfully
                println("CustomTabs - Navigation Finished")
            }
            NAVIGATION_FAILED -> {
                // URL loading failed
                println("CustomTabs - Navigation Failed")
            }
            NAVIGATION_ABORTED -> {
                // URL loading was aborted
                println("CustomTabs - Navigation Aborted")
            }
            TAB_HIDDEN -> {
                println("CustomTabs - Tab Hidden")
            }
            TAB_SHOWN -> {
                println("CustomTabs - Tab Shown")
            }
        }
    }
}