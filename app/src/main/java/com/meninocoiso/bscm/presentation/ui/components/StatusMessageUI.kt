package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R

enum class Size(
    val icon: Int,
    val gap: Int,
    val title: @Composable () -> TextStyle,
    val message: @Composable () -> TextStyle
) {
    Medium(
        icon = 56,
        gap = 16,
        title = { MaterialTheme.typography.titleLarge },
        message = { MaterialTheme.typography.bodyMedium }
    ),
    Small(
        icon = 24,
        gap = 4,
        title = { MaterialTheme.typography.titleSmall },
        message = { MaterialTheme.typography.bodySmall }
    )
}

@Preview
@Composable
fun StatusMessagePreviewUI() {
    StatusMessageUI(
        title = "No internet connection",
        message = "Please check your connection and try again",
        icon = R.drawable.rounded_wifi_off_24,
        onClick = {},
        buttonLabel = "Try again"
    )
}

@Composable
fun StatusMessageUI(
    title: String,
    message: String,
    icon: Int,
    modifier: Modifier? = Modifier,
    size: Size = Size.Medium,
    onClick: (() -> Unit)? = null,
    buttonLabel: String = "Try again"
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp).let {
            if (modifier != null) it.then(modifier) else it
        },
        verticalArrangement = Arrangement.spacedBy(
            space = size.gap.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = icon),
            modifier = Modifier.size(size.icon.dp),
            contentDescription = ""
        )
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = size.title(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = size.message(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
        if (onClick != null) {
            OutlinedButton(
                onClick = { onClick() },
            ) {
                Text(buttonLabel)
            }
        }
    }
}