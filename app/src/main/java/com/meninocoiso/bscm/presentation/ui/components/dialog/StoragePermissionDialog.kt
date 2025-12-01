package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import com.meninocoiso.bscm.util.StorageUtils.INITIAL_URL

@Composable
fun StoragePermissionDialog(
    onPermissionGranted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    
    var incorrectPermissionState by remember {
        mutableStateOf(false)
    }

    val folderPickerLauncher = StorageUtils.folderPickerLauncher(
        context,
        validate = { uri -> uri == BEATSTAR_URI },
        onPermissionGranted = { onPermissionGranted() },
        onInvalidSelection = { incorrectPermissionState = true }
    )
    
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(
                    R.drawable.rounded_folder_limited_24
                ),
                modifier = Modifier.size(24.dp),
                contentDescription = "Storage permission icon"
            )
        },
        title = {
            Text(
                text = stringResource(if (incorrectPermissionState) {
                    R.string.incorrect_storage_permission
                } else {
                    R.string.storage_permission_required
                }),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(if (incorrectPermissionState) {
                    R.string.incorrect_storage_permission_description
                } else {
                    R.string.storage_permission_required_description
                }
            ))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    folderPickerLauncher.launch(INITIAL_URL)
                }
            ) {
                Text(stringResource(if (incorrectPermissionState) {
                    R.string.try_again
                } else {
                    R.string.select_folder
                }))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}