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
    val isPublic: Boolean,
    val owner: SimplifiedUser,
    val itemCount: Triple<Int, Int, Int> = Triple(0, 0, 0), // chartCount, tourPassCount, themeCount
) : Parcelable

/**
 * Converts a [Collection] that already carries its [owner] (e.g. fetched via a deep-link
 * where the server includes the owner in the response).
 *
 * @throws IllegalStateException if the collection has no owner.
 */
fun Collection.toSimplifiedCollection(): SimplifiedCollection {
    val resolvedOwner = checkNotNull(owner) {
        "Collection '$id' has no owner — use toSimplifiedCollection(user) to supply one explicitly."
    }
    return toSimplifiedCollection(resolvedOwner)
}

/**
 * Converts a [Collection] using an explicitly provided [user] as the owner.
 * Use this when the owner is already known in the call site (e.g. Profile screens)
 * and the server did not return the owner inside the collection payload.
 */
fun Collection.toSimplifiedCollection(user: SimplifiedUser) = SimplifiedCollection(
    id = id,
    name = name,
    slug = slug,
    isPublic = isPublic,
    coverUrl = coverUrl,
    owner = user,
    itemCount = Triple(chartCount, tourPassCount, themeCount)
)

