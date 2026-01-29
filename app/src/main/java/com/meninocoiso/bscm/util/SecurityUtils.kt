package com.meninocoiso.bscm.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    /**
     * Gets the SHA-256 hash of signing certificate.
     */
    fun getAppSignature(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners[0]
            } else {
                @Suppress("DEPRECATION")
                (packageInfo.signatures?.get(0))
            }

            if (signature == null) {
                return "error:signature_null"
            }

            val md = MessageDigest.getInstance("SHA-256")
            val certHash = md.digest(signature.toByteArray())
            Base64.encodeToString(certHash, Base64.NO_WRAP)
        } catch (e: Exception) {
            "error:${e.message}"
        }
    }

    /**
     * Signs data using HMAC-SHA256.
     * The secret is your app's certificate signature.
     */
    fun signData(context: Context, data: String): String {
        val secret = getAppSignature(context)
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val signature = mac.doFinal(data.toByteArray())
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }
}