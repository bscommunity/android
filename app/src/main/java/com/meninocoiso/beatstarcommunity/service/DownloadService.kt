package com.meninocoiso.beatstarcommunity.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.documentfile.provider.DocumentFile

class DownloadService : Service() {

	override fun onBind(intent: Intent?): IBinder? = null  // We're not binding.

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		// Retrieve the persisted tree URI from the intent.
		val treeUriString = intent?.getStringExtra("tree_uri")
		treeUriString?.let {
			val treeUri = Uri.parse(it)
			// Wrap the URI in a DocumentFile object to work with it as a folder.
			val rootDir = DocumentFile.fromTreeUri(this, treeUri)
			// Find or create the "beatstar" subfolder.
			val beatstarFolder = rootDir?.findFile("beatstar")
				?: rootDir?.createDirectory("beatstar")

			// For demonstration, create a dummy text file in the beatstar folder.
			beatstarFolder?.let { folder ->
				val newFile = folder.createFile("text/plain", "example.txt")
				newFile?.uri?.let { fileUri ->
					try {
						contentResolver.openOutputStream(fileUri)?.use { outputStream ->
							outputStream.write("Downloaded file content goes here.".toByteArray())
						}
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
		}
		// Stop the service after work is done.
		stopSelf()
		return START_NOT_STICKY
	}
}