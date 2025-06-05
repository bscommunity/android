package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.StreamingPlatform
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "streaming_links")
@Serializable
@Parcelize
data class StreamingLink(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "chart_id") val chartId: String,
    val platform: StreamingPlatform, 
    val url: String
) : Parcelable