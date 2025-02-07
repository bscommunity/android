package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import com.meninocoiso.beatstarcommunity.presentation.ui.components.RadioGroupUI
import java.util.Locale

enum class FeedbackType {
    BUG,
    SUGGESTION,
}

val FeedbackTypeStrings = mapOf<FeedbackType, String>(
    FeedbackType.BUG to "Bug",
    FeedbackType.SUGGESTION to "Suggestion"
)

@Preview
@Composable
fun FeedbackDialogPreview(
) {
    FeedbackDialog(
        onSubmit = { _, _ -> }
    )
}

private const val maxLength = 200

@Composable
fun FeedbackDialog(
    onSubmit: (
        feedbackType: FeedbackType,
        content: String
    ) -> Unit
) {
    val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

    var type by remember { mutableStateOf(FeedbackType.BUG) }
    var text by remember { mutableStateOf("") }

    Button(onClick = {
        setIsOpened(true)
    }) {
            Text(
                text = "Test"
            )
    }
    when {
        isOpened -> {
            AlertDialog(
                onDismissRequest = { setIsOpened(false) },
                title = {
                    Text(text = "Feedback")
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Help the chart contributors to improve by reporting bugs or suggesting new features.")
                        RadioGroupUI(
                            radioOptions = FeedbackType.entries.map {
                                FeedbackTypeStrings[it]!!
                            },
                            onOptionSelected = {
                                type = FeedbackType.valueOf(it.uppercase(Locale.ROOT))})
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                if (newText.length <= maxLength) {
                                    text = newText
                                }
                            },
                            label = { Text("Feedback") },
                            maxLines = 5,
                            minLines = 3,
                            supportingText = { Text("${text.length}/200") }
                        )
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
                    Button(onClick = {
                        onSubmit(FeedbackType.BUG, "Test")
                        setIsOpened(false)
                    }) {
                        Text(text = "Confirm")
                    }
                }
            )
        }
    }
}