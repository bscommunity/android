package com.meninocoiso.bscm.data.remote.dto.user

import kotlinx.serialization.Serializable

/**
 * Generic paginated response that pairs a page of items with a total domain count.
 */
@Serializable
data class ItemsPage<T>(
    val items: List<T>,
    val counts: ContentCounts? = null,
)

@Serializable
data class ContentCounts(
    val charts: Int = 0,
    val tourPasses: Int = 0,
    val themes: Int = 0,
    val collections: Int = 0,
)

/**
 * Per-section item counts returned inside the profile header response.
 * Maps to a JSON object: `{"charts": 5, "tourPasses": 0, "themes": 0}`.
 */
@Serializable
data class SectionCounts(
    val charts: Int = 0,
    val tourPasses: Int = 0,
    val themes: Int = 0,
) {
    fun toTriple(): Triple<Int, Int, Int> = Triple(charts, tourPasses, themes)
}

/**
 * Profile-level counts returned by the profile header endpoint.
 *
 * Each field is nullable — the server only includes fields that were requested via the
 * `counts` query parameter (e.g. `counts=library,likes,bookmarks,collections`).
 */
@Serializable
data class CountedResponses(
    val library: SectionCounts? = null,
    val likes: SectionCounts? = null,
    val bookmarks: SectionCounts? = null,
    val collections: Int? = null,
    val followers: Int? = null,
    val following: Int? = null,
)
