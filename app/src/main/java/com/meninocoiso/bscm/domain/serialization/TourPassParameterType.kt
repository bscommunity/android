package com.meninocoiso.bscm.domain.serialization

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.meninocoiso.bscm.domain.model.TourPass
import kotlinx.serialization.json.Json

internal val TourPassParameterType = object : NavType<TourPass>(
    isNullableAllowed = false
) {
    override fun put(bundle: Bundle, key: String, value: TourPass) {
        bundle.putParcelable(key, value)
    }

    override fun get(bundle: Bundle, key: String): TourPass? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, TourPass::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
    }

    override fun serializeAsValue(value: TourPass): String {
        // Serialized values must always be Uri encoded
        return Uri.encode(Json.encodeToString(value))
    }

    override fun parseValue(value: String): TourPass {
        // Navigation takes care of decoding the string
        // before passing it to parseValue()
        return Json.decodeFromString<TourPass>(value)
    }
}
