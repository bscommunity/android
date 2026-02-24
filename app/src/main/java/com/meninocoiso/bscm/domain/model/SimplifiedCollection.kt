package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
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
    val slug: String? = null,
    val coverUrl: String? = null,
    val owner: SimplifiedUser? = null,
    val itemCount: Triple<Int, Int, Int> = Triple(0, 0, 0), // chartCount, tourPassCount, themeCount
) : Parcelable

fun Collection.toSimplifiedCollection() = SimplifiedCollection(
    id = id,
    name = name,
    slug = slug,
    coverUrl = coverUrl,
    owner = owner,
    itemCount = Triple(chartCount, tourPassCount, themeCount)
)


