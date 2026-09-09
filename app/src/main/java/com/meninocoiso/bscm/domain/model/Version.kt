package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Entity(tableName = "versions")
@Serializable
@Parcelize
data class Version(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "catalog_item_id") val catalogItemId: String,
    @ColumnInfo(name = "version_code") val versionCode: Int,
    @ColumnInfo(name = "downloads_amount") val downloadsAmount: Int = 0,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long,
    @ColumnInfo(name = "changelog") val changelog: String? = null,
    @ColumnInfo(name = "discord_attachment_id") val discordAttachmentId: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "bundle_hash") val bundleHash: String? = null,
) : Parcelable
