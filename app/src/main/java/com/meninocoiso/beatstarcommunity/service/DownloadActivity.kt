package com.meninocoiso.beatstarcommunity.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text

class DownloadActivity : ComponentActivity() {
    // Define the launcher for picking a directory.
    private val openDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let {
            // Persist the permission for future access.
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // Start the service and pass the URI as a string extra.
            val serviceIntent = Intent(this, DownloadService::class.java).apply {
                putExtra("tree_uri", it.toString())
            }
            startService(serviceIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Simple UI with a button to open the directory picker.
            Button(onClick = { openDirectoryLauncher.launch(null) }) {
                Text("Select Download Directory")
            }
        }
    }
}
