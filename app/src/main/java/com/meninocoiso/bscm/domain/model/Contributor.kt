package com.meninocoiso.bscm.domain.model

import LocalDateTimeSerializer
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.enums.Role
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "contributors")
@Serializable
@Parcelize
data class Contributor(
    @PrimaryKey val user: ContributorUserDto,
    val chartId: String,
    val roles: List<Role>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val joinedAt: LocalDateTime
) : Parcelable