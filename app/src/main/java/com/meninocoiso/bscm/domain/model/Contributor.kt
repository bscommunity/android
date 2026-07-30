package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.enums.Role
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Entity(tableName = "contributors")
@Serializable
@Parcelize
data class Contributor(
    @PrimaryKey val user: SimplifiedUser,
    @ColumnInfo(name = "catalog_item_id") val catalogItemId: String,
    val note: String? = null,
    val role: Role,
    @Serializable(with = LocalDateTimeSerializer::class) val joinedAt: LocalDateTime,
) : Parcelable
