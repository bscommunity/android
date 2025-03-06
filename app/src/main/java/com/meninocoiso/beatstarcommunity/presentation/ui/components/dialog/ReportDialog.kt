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
import java.util.Locale

enum class ReportType {
    EXPLICIT_CONTENT,
    VIOLENT_CONTENT,
    SPAM,
    INTELLECTUAL_PROPERTY_VIOLATION,
}

val ReportTypeStrings = mapOf<ReportType, String>(
    ReportType.EXPLICIT_CONTENT to "Explicit content",
    ReportType.VIOLENT_CONTENT to "Violent content",
    ReportType.SPAM to "Spam",
    ReportType.INTELLECTUAL_PROPERTY_VIOLATION to "Intellectual property"
)

@Preview
@Composable
fun ReportDialogPreview(
) {
    ReportDialog(
        onSubmit = {}
    )
}

@Composable
fun ReportDialog(
    onSubmit: (
        reportType: ReportType,
    ) -> Unit
) {
    val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

    var type by remember { mutableStateOf<ReportType?>(null) }

    Button(onClick = {
        setIsOpened(true)
    }) {
            Text(
                text = "Report Test"
            )
    }
    when {
        isOpened -> {
            AlertDialog(
                onDismissRequest = { setIsOpened(false) },
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
                            radioOptions = ReportType.entries.map {
                                ReportTypeStrings[it]!!
                            },
                            onOptionSelected = {
                                type = ReportType.valueOf(it.uppercase(Locale.ROOT))})
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        setIsOpened(false)
                    }) {
                        Text(text = "Cancel")
                    }
                },
                confirmButton = {
                    Button(
                        enabled = type != null,
                        onClick = {
                            onSubmit(type!!)
                            setIsOpened(false)
                        }
                    ) {
                        Text(text = "Confirm")
                    }
                }
            )
        }
    }
}