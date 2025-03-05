package com.meninocoiso.beatstarcommunity.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R

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
    onClick: (() -> Unit)? = null,
    buttonLabel: String = "Try again"
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = icon),
            modifier = Modifier.size(56.dp),
            contentDescription = ""
        )
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
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