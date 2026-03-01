package com.meninocoiso.bscm.presentation.ui.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.domain.result.UiText

/**
 * Resolve a [UiText] into a concrete String inside a composable.
 */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.StringResource -> stringResource(this.resId, *this.args.toTypedArray())
    is UiText.DynamicString -> this.value
}

// For non-Composable contexts (LaunchedEffect, lambdas, ViewModels)
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.StringResource -> context.getString(resId, *args.toTypedArray())
    is UiText.DynamicString -> value
}