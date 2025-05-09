package com.meninocoiso.beatstarcommunity.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.meninocoiso.beatstarcommunity.domain.enums.UpdatesSection")
enum class UpdatesSection {
    @SerialName("Workshop")
    Workshop,
    @SerialName("Installations")
    Installations
}