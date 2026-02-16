package com.meninocoiso.bscm.domain.serialization

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import kotlinx.serialization.json.Json

internal val SimplifiedUserParameterType = object : NavType<SimplifiedUser>(
    isNullableAllowed = false
) {
    override fun put(bundle: Bundle, key: String, value: SimplifiedUser) {
        bundle.putParcelable(key, value)
    }

    override fun get(bundle: Bundle, key: String): SimplifiedUser? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, SimplifiedUser::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
    }

    override fun serializeAsValue(value: SimplifiedUser): String {
        // Serialized values must always be Uri encoded
        return Uri.encode(Json.encodeToString(value))
    }

    override fun parseValue(value: String): SimplifiedUser {
        // Navigation takes care of decoding the string
        // before passing it to parseValue()
        return Json.decodeFromString<SimplifiedUser>(value)
    }
}