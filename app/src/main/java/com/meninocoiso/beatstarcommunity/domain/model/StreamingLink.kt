package com.meninocoiso.beatstarcommunity.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class StreamingLink(val platform: String, val url: String) : Parcelable