package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.meninocoiso.bscm.domain.enums.ButtonVariant

@Composable
fun ButtonUI(
    text: String,
    icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Filled,
) {
    val hasIcon = icon != null

    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = onClick,
            contentPadding = if (hasIcon) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
            modifier = modifier,
            enabled = enabled,
        ) {
            if (hasIcon) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            }
            Text(text)
        }
        ButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            contentPadding = if (hasIcon) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
            modifier = modifier,
            enabled = enabled,
        ) {
            if (hasIcon) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            }
            Text(text)
        }
        ButtonVariant.Tonal -> FilledTonalButton(
            onClick = onClick,
            contentPadding = if (hasIcon) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
            modifier = modifier,
            enabled = enabled,
        ) {
            if (hasIcon) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            }
            Text(text)
        }
    }
}