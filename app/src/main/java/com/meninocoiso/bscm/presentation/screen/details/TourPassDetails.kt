package com.meninocoiso.bscm.presentation.screen.details

import com.meninocoiso.bscm.domain.model.TourPass
import kotlinx.serialization.Serializable

@Serializable
data class TourPassDetails(val tourPass: TourPass)

@Serializable
data class DeepLinkTourPassDetails(val contentId: String)
