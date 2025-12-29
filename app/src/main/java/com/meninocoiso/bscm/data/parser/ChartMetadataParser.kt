package com.meninocoiso.bscm.data.parser

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val TAG = "ChartMetadataParser"

/**
 * Chart-specific implementation of metadata parser
 */
class ChartMetadataParser @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ContentMetadataParser<ExternalContentMetadata> {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun parseMetadata(file: DocumentFile): ExternalContentMetadata? {
        return try {
            context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                var jsonText = reader.readText().trim()

                // Quick regex to wrap unquoted numeric IDs with quotes
                // Matches: "id":123 or "id":9995564566578350000
                // Replaces with: "id":"123" or "id":"9995564566578350000"
                jsonText = jsonText.replace(
                    Regex(""""id"\s*:\s*(\d+)"""),
                    """"id":"$1""""
                )

                json.decodeFromString<ExternalContentMetadata>(jsonText)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse metadata from ${file.name}", e)
            null
        }
    }
}

