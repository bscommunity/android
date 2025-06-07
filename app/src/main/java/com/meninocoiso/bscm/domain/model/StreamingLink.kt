package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.StreamingPlatform
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "streaming_links")
@Serializable
@Parcelize
data class StreamingLink(
    val platform: StreamingPlatform,
    @PrimaryKey val url: String
) : Parcelable