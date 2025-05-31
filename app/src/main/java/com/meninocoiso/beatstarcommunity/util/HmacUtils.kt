package com.meninocoiso.beatstarcommunity.util

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtils {
    fun generateHmacSHA256(data: String, secret: String): String {
        val hmacSHA256 = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        hmacSHA256.init(secretKey)
        val hash = hmacSHA256.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }
}
