package com.meninocoiso.beatstarcommunity.domain.serialization

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import kotlinx.serialization.json.Json

internal val ChartParameterType = object : NavType<Chart>(
    isNullableAllowed = false
) {
    override fun put(bundle: Bundle, key: String, value: Chart) {
        bundle.putParcelable(key, value)
    }
    override fun get(bundle: Bundle, key: String): Chart? {
        return bundle.getParcelable(key) as Chart?
    }

    override fun serializeAsValue(value: Chart): String {
        // Serialized values must always be Uri encoded
        return Uri.encode(Json.encodeToString(value))
    }

    override fun parseValue(value: String): Chart {
        // Navigation takes care of decoding the string
        // before passing it to parseValue()
        return Json.decodeFromString<Chart>(value)
    }
}