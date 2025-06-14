package com.meninocoiso.bscm.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.domain.enums.ThemePreference
import com.meninocoiso.bscm.domain.model.internal.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
		val ALLOW_GAMEPLAY_PREVIEW_VIDEO = booleanPreferencesKey("allow_gameplay_preview_video")
		val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
		val THEME = stringPreferencesKey("theme")
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

	suspend fun setGameplayPreviewVideo(allow: Boolean) = 
		dataStore.edit { it[ALLOW_GAMEPLAY_PREVIEW_VIDEO] = allow }
		
	suspend fun setDynamicColors(use: Boolean) =
		dataStore.edit { it[USE_MATERIAL_YOU] = use }

	suspend fun setAppTheme(theme: ThemePreference) =
		dataStore.edit { it[THEME] = theme.name }
	

	private fun mapSettings(preferences: Preferences): Settings = Settings(
		allowExplicitContent = preferences[ALLOW_EXPLICIT_CONTENT]
			?: Settings().allowExplicitContent,
		enableGameplayPreviewVideo = preferences[ALLOW_GAMEPLAY_PREVIEW_VIDEO]
			?: Settings().enableGameplayPreviewVideo,
		useDynamicColors = preferences[USE_MATERIAL_YOU]
			?: Settings().useDynamicColors,
		theme = preferences[THEME]?.let { ThemePreference.valueOf(it) }
			?: Settings().theme,
	)
}