package com.meninocoiso.beatstarcommunity.data.local

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import com.meninocoiso.beatstarcommunity.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Class that handles saving and retrieving user preferences
 */
class UserPreferencesRepository @Inject constructor(
	private val dataStore: DataStore<Preferences>
) {
	private val tag = this::class.java.simpleName

	private object PreferencesKeys {
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
			preferences[PreferencesKeys.ALLOW_EXPLICIT_CONTENT] = allow
		}
	}

	/**
	 * Enable / disable use of Material You
	 */
	suspend fun useDynamicColors(use: Boolean) {
		// Data updates are handled transactionally, ensuring that if the permission is
		// updated at the same time from another thread, we won't have conflicts
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.USE_MATERIAL_YOU] = use
		}
	}

	suspend fun updateAppTheme(theme: ThemePreference) {
		println("Theme: $theme")
		dataStore.edit { preferences ->
			preferences[PreferencesKeys.THEME] = theme.name
		}
	}

	/**
	 * Get the stream of UserPreferences
	 */
	val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
		.catch { exception ->
			// "dataStore.data" throws an IOException when an error is encountered when reading data
			if (exception is IOException) {
				Log.e(tag, "Error reading preferences.", exception)
				emit(emptyPreferences())
			} else {
				throw exception
			}
		}.map { preferences ->
			mapUserPreferences(preferences)
		}

	private fun mapUserPreferences(preferences: Preferences): UserPreferences {
		// Get the theme from preferences and convert it to a [ThemePreference] object
		val theme =
			ThemePreference.valueOf(
				preferences[PreferencesKeys.THEME] ?: ThemePreference.SYSTEM.name
			)

		// Get our boolean values, defaulting to false if not set:
		val allowExplicitContent = preferences[PreferencesKeys.ALLOW_EXPLICIT_CONTENT] ?: false
		val useDynamicColors = preferences[PreferencesKeys.USE_MATERIAL_YOU] ?: true

		return UserPreferences(allowExplicitContent, useDynamicColors, theme)
	}
}