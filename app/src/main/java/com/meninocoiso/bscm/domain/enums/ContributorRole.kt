package com.meninocoiso.bscm.domain.enums
import kotlinx.serialization.Serializable

@Serializable
enum class ContributorRole {
    AUTHOR, CHART, AUDIO, REVISION, EFFECTS, SYNC, PREVIEW
}