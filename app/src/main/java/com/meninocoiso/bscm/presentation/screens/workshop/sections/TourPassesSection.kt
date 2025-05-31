package com.meninocoiso.bscm.presentation.screens.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI

@Composable
internal fun TourPassesSection(nestedScrollConnection: NestedScrollConnection) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessageUI(
            title = "Work in progress!",
            message = "This feature still needs some work\nPlease, check back later",
            icon = R.drawable.rounded_hourglass_24,
        )
    }
}