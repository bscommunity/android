package com.meninocoiso.bscm.data.remote.dto.collection

import com.meninocoiso.bscm.domain.enums.CollectionKind

data class SimplifiedCollection (
    val id: String,
    val kind: CollectionKind,
)