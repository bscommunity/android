package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import com.meninocoiso.bscm.domain.enums.Genre
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val isrc: String? = null,
    val genres: List<Genre> = emptyList(),
    val bpm: Int? = null,
    val duration: Float,
    val streamingRefs: List<StreamingRef> = emptyList(),
    val coverUrl: String? = null,
    val previewUrl: String? = null,
) : Parcelable
