package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.RadioGroupUI

enum class FeedbackType {
    BUG,
    SUGGESTION,
}

@Composable
fun getFeedbackTypeString(): Map<FeedbackType, String> {
    return mapOf<FeedbackType, String>(
        FeedbackType.BUG to stringResource(R.string.bug),
        FeedbackType.SUGGESTION to stringResource(R.string.suggestion)
    )
}

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
    
    val feedbackTypeStrings = getFeedbackTypeString()

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
                    Text(text = stringResource(R.string.feedback))
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = stringResource(R.string.feedback_description))
                        RadioGroupUI(
                            radioOptions = FeedbackType.entries.map {
                                feedbackTypeStrings[it]!!
                            },
                            onOptionSelected = { index, _ ->
                                type = FeedbackType.entries.elementAt(index)
                            }
                        )
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                if (newText.length <= maxLength) {
                                    text = newText
                                }
                            },
                            label = { Text(stringResource(R.string.feedback)) },
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
                        Text(text = stringResource(R.string.cancel))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onSubmit(FeedbackType.BUG, "Test")
                        setIsOpened(false)
                    }) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }
            )
        }
    }
}