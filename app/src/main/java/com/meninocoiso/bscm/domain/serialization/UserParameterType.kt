package com.meninocoiso.bscm.domain.serialization

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.meninocoiso.bscm.domain.model.User
import kotlinx.serialization.json.Json

internal val UserParameterType = object : NavType<User>(
    isNullableAllowed = false
) {
    override fun put(bundle: Bundle, key: String, value: User) {
        bundle.putParcelable(key, value)
    }

    override fun get(bundle: Bundle, key: String): User? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, User::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
    }

    override fun serializeAsValue(value: User): String {
        // Serialized values must always be Uri encoded
        return Uri.encode(Json.encodeToString(value))
    }

    override fun parseValue(value: String): User {
        // Navigation takes care of decoding the string
        // before passing it to parseValue()
        return Json.decodeFromString<User>(value)
    }
}