package com.meninocoiso.bscm.presentation.ui.components.dialog

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.ReportType
import com.meninocoiso.bscm.presentation.ui.components.RadioGroupUI

@Composable
private fun getReportTypeString(): Map<ReportType, String> {
    return mapOf(
        ReportType.EXPLICIT_CONTENT to stringResource(R.string.explicit_content),
        ReportType.VIOLENT_CONTENT to stringResource(R.string.violent_content),
        ReportType.SPAM to stringResource(R.string.spam),
        ReportType.INTELLECTUAL_PROPERTY to stringResource(R.string.intellectual_property)
    )
}

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
    val reportTypeStrings = getReportTypeString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.report))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stringResource(R.string.report_description))
                RadioGroupUI(
                    initialSelected = "",
                    radioOptions = ReportType.entries.map {
                        reportTypeStrings[it]!!
                    },
                    onOptionSelected = { index, _ ->
                        type = ReportType.entries.elementAt(index)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
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
                Text(text = stringResource(R.string.confirm))
            }
        }
    )
}