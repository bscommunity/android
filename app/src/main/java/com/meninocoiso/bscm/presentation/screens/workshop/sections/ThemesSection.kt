package com.meninocoiso.bscm.presentation.screens.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI

@Composable
internal fun ThemesSection(nestedScrollConnection: NestedScrollConnection) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessageUI(
            title = stringResource(R.string.work_in_progress),
            message = stringResource(R.string.work_in_progress_description),
            icon = R.drawable.rounded_hourglass_24,
        )
    }
}