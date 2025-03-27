package com.meninocoiso.beatstarcommunity.util

import android.os.Build

class DevelopmentUtils {
    companion object {
        fun isEmulator(): Boolean {
            return (Build.FINGERPRINT.contains("generic") ||
                    Build.FINGERPRINT.contains("unknown") ||
                    Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK built for x86") ||
                    Build.MANUFACTURER.contains("Genymotion") ||
                    (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                    "google_sdk" == Build.PRODUCT)
        }
    }
}