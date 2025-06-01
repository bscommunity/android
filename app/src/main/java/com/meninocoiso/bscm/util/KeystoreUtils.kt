package com.meninocoiso.bscm.util

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object KeystoreUtils {
    init {
        System.loadLibrary("bscm")
    }

    external fun getApiSecret(): String
    
    fun signData(data: String): String {
        val secret = getApiSecret()
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val signature = mac.doFinal(data.toByteArray())
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }
}
