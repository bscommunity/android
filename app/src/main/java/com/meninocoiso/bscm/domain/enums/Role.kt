package com.meninocoiso.bscm.domain.enums
import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    AUTHOR, CHART, AUDIO, REVISION, EFFECTS, SYNC, GAMEPLAY, ART, TEXTURES
}