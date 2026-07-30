package com.meninocoiso.bscm.domain.enums

import kotlinx.serialization.Serializable

@Serializable
enum class Visibility {
    PUBLIC,
    UNLISTED,
    PRIVATE,
}
