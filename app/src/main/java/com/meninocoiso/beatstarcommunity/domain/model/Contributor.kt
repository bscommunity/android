package com.meninocoiso.beatstarcommunity.domain.model

import LocalDateSerializer
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.beatstarcommunity.data.remote.dto.ContributorUserDto
import com.meninocoiso.beatstarcommunity.domain.enums.ContributorRole
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "contributors")
@Serializable
@Parcelize
data class Contributor(
    @PrimaryKey val user: ContributorUserDto,
    val chartId: String,
    val roles: List<ContributorRole>,
    @Serializable(with = LocalDateSerializer::class)
    val joinedAt: LocalDate
) : Parcelable