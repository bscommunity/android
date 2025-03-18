package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.presentation.ui.components.RadioGroupUI

enum class ReportType {
    EXPLICIT_CONTENT,
    VIOLENT_CONTENT,
    SPAM,
    INTELLECTUAL_PROPERTY
}

val ReportTypeStrings = mapOf(
    ReportType.EXPLICIT_CONTENT to "Explicit content",
    ReportType.VIOLENT_CONTENT to "Violent content",
    ReportType.SPAM to "Spam",
    ReportType.INTELLECTUAL_PROPERTY to "Intellectual property"
)

@Preview
@Composable
fun ReportDialogPreview(
) {
    val isOpened = remember { mutableStateOf(true) }

    Button(
        onClick = {
            isOpened.value = true
        }
    ) {
        Text(
            text = "Open Report Dialog"
        )
    }

    ReportDialog(
        onDismiss = {
            isOpened.value = false
        },
        onSubmit = {}
    )
}

@Composable
fun ReportDialog(
    onDismiss: () -> Unit = {},
    onSubmit: (
        reportType: ReportType,
    ) -> Unit
) {
    var type by remember { mutableStateOf<ReportType?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Report")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Help the community by flagging inappropriate content\nYour report is anonymous")
                RadioGroupUI(
                    initialSelected = "",
                    radioOptions = ReportType.entries.map {
                        ReportTypeStrings[it]!!
                    },
                    onOptionSelected = { index, _ ->
                        type = ReportType.entries.elementAt(index)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(
                enabled = type != null,
                onClick = {
                    onSubmit(type!!)
                    onDismiss()
                }
            ) {
                Text(text = "Confirm")
            }
        }
    )
}