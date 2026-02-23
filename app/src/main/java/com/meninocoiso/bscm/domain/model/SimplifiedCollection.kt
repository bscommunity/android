package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Lightweight snapshot of a [Collection] passed between navigation destinations,
 * so the Collection screen can display basic info (name, cover) while items load.
 *
 * Mirrors the SimplifiedUser pattern used by the Profile screen.
 */
@Serializable
@Parcelize
data class SimplifiedCollection(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val itemCount: Int = 0,
) : Parcelable

fun Collection.toSimplifiedCollection() = SimplifiedCollection(
    id = id,
    name = name,
    coverUrl = coverUrl,
    itemCount = chartCount,
)


