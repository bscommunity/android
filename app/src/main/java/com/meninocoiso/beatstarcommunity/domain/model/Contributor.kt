package com.meninocoiso.beatstarcommunity.domain.model

import LocalDateSerializer
import com.meninocoiso.beatstarcommunity.data.remote.dto.ContributorUserDto
import com.meninocoiso.beatstarcommunity.domain.enums.ContributorRoleEnum
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Contributor(
    val user: ContributorUserDto,
    val chartId: String,
    val roles: List<ContributorRoleEnum>,
    @Serializable(with = LocalDateSerializer::class)
    val joinedAt: LocalDate
)