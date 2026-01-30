package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.meninocoiso.bscm.R

@Composable
fun ButtonUI(
    text: String,
    icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val hasIcon = icon != null

    Button(
        onClick = onClick,
        contentPadding = if (hasIcon) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
        modifier = modifier,
        enabled = enabled,
    ) {
        if (hasIcon) {
            Icon(
                painter = painterResource(R.drawable.outline_settings_24),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(text)
    }
}