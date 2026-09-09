package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.StreamingPlatform
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "streaming_refs")
@Serializable
@Parcelize
data class StreamingRef(
    val platform: StreamingPlatform,
    val url: String,
    @PrimaryKey @ColumnInfo(name = "id") val id: String = url,
) : Parcelable
