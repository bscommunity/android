package com.meninocoiso.bscm.domain.model.internal

import androidx.compose.material3.SnackbarDuration

data class SnackbarProps(
    val message: String,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val duration: SnackbarDuration =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
)
