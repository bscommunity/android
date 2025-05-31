package com.meninocoiso.bscm.presentation.ui.modifiers

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.debouncedClickable(
    durationMillis: Long = 400,
    onClick: () -> Unit,
): Modifier {
    val lastClickTime = remember { mutableLongStateOf(0L) }
    
    return clickable { 
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime.value < durationMillis) return@clickable
        lastClickTime.value = currentTime
        onClick()
    }
}