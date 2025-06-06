package com.meninocoiso.bscm.domain.model

import LocalDateSerializer
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.enums.ContributorRole
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