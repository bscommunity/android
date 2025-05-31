package com.meninocoiso.bscm.domain.enums

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Keep
enum class UpdatesSection {
    @SerialName("Workshop")
    Workshop,
    @SerialName("Installations")
    Installations
}