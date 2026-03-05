package com.meninocoiso.bscm.domain.serialization

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.navigation.NavType
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import kotlinx.serialization.json.Json

internal val SimplifiedCollectionParameterType = object : NavType<SimplifiedCollection>(
    isNullableAllowed = false
) {
    override fun put(bundle: Bundle, key: String, value: SimplifiedCollection) {
        bundle.putParcelable(key, value)
    }

    override fun get(bundle: Bundle, key: String): SimplifiedCollection? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, SimplifiedCollection::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
    }

    override fun serializeAsValue(value: SimplifiedCollection): String {
        return Uri.encode(Json.encodeToString(value))
    }

    override fun parseValue(value: String): SimplifiedCollection {
        return Json.decodeFromString<SimplifiedCollection>(value)
    }
}

