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
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Class that handles saving and retrieving user preferences
 */
class SettingsRepository @Inject constructor(
	private val dataStore: DataStore<Preferences>
) {
	private val tag = this::class.java.simpleName

	private object SettingsKeys {
		val ALLOW_EXPLICIT_CONTENT = booleanPreferencesKey("allow_explicit_content")
		val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
		val THEME = stringPreferencesKey("theme")
	}

	/**
	 * Enable / disable permission of explicit content
	 */
	suspend fun allowExplicitContent(allow: Boolean) {
		// Data updates are handled transactionally, ensuring that if the permission is
		// updated at the same time from another thread, we won't have conflicts
		dataStore.edit { preferences ->
			preferences[SettingsKeys.ALLOW_EXPLICIT_CONTENT] = allow
		}
	}

	/**
	 * Enable / disable use of Material You
	 */
	suspend fun useDynamicColors(use: Boolean) {
		// Data updates are handled transactionally, ensuring that if the permission is
		// updated at the same time from another thread, we won't have conflicts
		dataStore.edit { preferences ->
			preferences[SettingsKeys.USE_MATERIAL_YOU] = use
		}
	}

	suspend fun updateAppTheme(theme: ThemePreference) {
		println("Theme: $theme")
		dataStore.edit { preferences ->
			preferences[SettingsKeys.THEME] = theme.name
		}
	}

	/**
	 * Get the stream of Settings
	 */
	val settingsFlow: Flow<Settings> = dataStore.data
		.catch { exception ->
			// "dataStore.data" throws an IOException when an error is encountered when reading data
			if (exception is IOException) {
				Log.e(tag, "Error reading preferences.", exception)
				emit(emptyPreferences())
			} else {
				throw exception
			}
		}.map { preferences ->
			mapSettings(preferences)
		}

	private fun mapSettings(preferences: Preferences): Settings {
		// Get the theme from preferences and convert it to a [ThemePreference] object
		val theme =
			ThemePreference.valueOf(
				preferences[SettingsKeys.THEME] ?: ThemePreference.SYSTEM.name
			)

		// Get our boolean values, defaulting to false if not set:
		val allowExplicitContent = preferences[SettingsKeys.ALLOW_EXPLICIT_CONTENT] ?: false
		val useDynamicColors = preferences[SettingsKeys.USE_MATERIAL_YOU] ?: true

		return Settings(allowExplicitContent, useDynamicColors, theme)
	}
}