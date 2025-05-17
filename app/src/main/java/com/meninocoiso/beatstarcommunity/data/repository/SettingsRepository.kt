package com.meninocoiso.beatstarcommunity.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import com.meninocoiso.beatstarcommunity.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
	private val dataStore: DataStore<Preferences>,
) {
	companion object SettingsKeys {
		val ALLOW_EXPLICIT_CONTENT = booleanPreferencesKey("allow_explicit_content")
		val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
		val THEME = stringPreferencesKey("theme")
		val FOLDER_URI = stringPreferencesKey("folder_uri")
		val LATEST_UPDATE_VERSION = stringPreferencesKey("app_update_version")
		val LATEST_CLEANED_VERSION = stringPreferencesKey("latest_cleaned_version")
	}

	val settingsFlow: Flow<Settings> = dataStore.data
		.catch { exception ->
			when (exception) {
				is IOException -> {
					Log.e("SettingsRepository", "Error reading preferences", exception)
					emit(emptyPreferences())
				}
				else -> throw exception
			}
		}
		.map { preferences ->
			mapSettings(preferences)
		}

	suspend fun setExplicitContent(allow: Boolean) =
		dataStore.edit { it[ALLOW_EXPLICIT_CONTENT] = allow }

	suspend fun setDynamicColors(use: Boolean) =
		dataStore.edit { it[USE_MATERIAL_YOU] = use }

	suspend fun setAppTheme(theme: ThemePreference) =
		dataStore.edit { it[THEME] = theme.name }

	suspend fun setFolderUri(uri: String) =
		dataStore.edit { it[FOLDER_URI] = uri }

	suspend fun getFolderUri(): String? {
		return dataStore.data.first()[FOLDER_URI]
	}

	suspend fun getLatestVersion(): String {
		return dataStore.data.first()[LATEST_UPDATE_VERSION] ?: ""
	}

	suspend fun setLatestVersion(version: String) {
		dataStore.edit { it[LATEST_UPDATE_VERSION] = version }
	}

	suspend fun getLatestCleanedVersion(): Int {
		return dataStore.data.first()[LATEST_CLEANED_VERSION]?.toIntOrNull() ?: 0
	}

	suspend fun setLatestCleanedVersion(version: Int) {
		dataStore.edit { it[LATEST_CLEANED_VERSION] = version.toString() }
	}

	private fun mapSettings(preferences: Preferences): Settings = Settings(
		allowExplicitContent = preferences[ALLOW_EXPLICIT_CONTENT]
			?: Settings().allowExplicitContent,
		useDynamicColors = preferences[USE_MATERIAL_YOU]
			?: Settings().useDynamicColors,
		theme = preferences[THEME]?.let { ThemePreference.valueOf(it) }
			?: Settings().theme,
		folderUri = preferences[FOLDER_URI]
			?: Settings().folderUri,
		latestUpdateVersion = preferences[LATEST_UPDATE_VERSION].let { 
			if (it.isNullOrEmpty()) null else it
		} ?: Settings().latestUpdateVersion
	)
}