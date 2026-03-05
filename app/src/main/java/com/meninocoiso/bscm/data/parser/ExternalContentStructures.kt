package com.meninocoiso.bscm.data.parser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic external content metadata (extracted from files like info.json)
 */
@Serializable
data class ExternalContentMetadata(
    val title: String,
    val artist: String,
    val id: String,
    val difficulty: Int? = null,
    val bpm: Double? = null,
    val maxScore: Int? = null,
    val type: String? = null,
    val contentId: String? = null,
    val duration: Float? = null,
    val notes: Int? = null,
    val effects: Int? = null,
    val contributors: String? = null,
    val publishedAt: Long? = null,
    val streaming: String? = null,
    val cover: String? = null,
    val gameplay: String? = null
)

/**
 * Generic color gradient for styling
 */
@Serializable
data class ColorGradient(
    val color: String,
    val time: Float
)

/**
 * Generic template configuration (extracted from config.json)
 */
@Serializable
data class ContentTemplate(
    @SerialName("BaseColor") val baseColor: String,
    @SerialName("DarkColor") val darkColor: String,
    @SerialName("ColorGradient") val colorGradient: List<ColorGradient>
)

/**
 * Generic content configuration
 */
@Serializable
data class ExternalContentConfig(
    @SerialName("SongTemplate") val songTemplate: ContentTemplate
)

