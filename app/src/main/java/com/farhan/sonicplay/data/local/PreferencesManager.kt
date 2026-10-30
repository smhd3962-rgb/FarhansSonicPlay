package com.farhan.sonicplay.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.farhan.sonicplay.data.model.ThemeMode
import com.farhan.sonicplay.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
        val ACTIVE_QUEUE_ID = intPreferencesKey("active_queue_id")
        val PAUSE_HEADSET = booleanPreferencesKey("pause_headset")
        val RESUME_HEADSET = booleanPreferencesKey("resume_headset")
        val SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val SLEEP_FINISH_SONG = booleanPreferencesKey("sleep_finish_song")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val themeModeString = preferences[Keys.THEME_MODE] ?: ThemeMode.AMOLED.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeString)
            } catch (e: Exception) {
                ThemeMode.AMOLED
            }
            UserPreferences(
                themeMode = themeMode,
                useDynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true,
                accentColorHex = preferences[Keys.ACCENT_COLOR_HEX] ?: "#00E5FF",
                activeQueueId = preferences[Keys.ACTIVE_QUEUE_ID] ?: 1,
                pauseOnHeadsetDisconnect = preferences[Keys.PAUSE_HEADSET] ?: true,
                resumeOnHeadsetConnect = preferences[Keys.RESUME_HEADSET] ?: false,
                shuffleMode = preferences[Keys.SHUFFLE_MODE] ?: false,
                repeatMode = preferences[Keys.REPEAT_MODE] ?: 0,
                crossfadeMs = preferences[Keys.CROSSFADE_MS] ?: 0,
                excludedFolders = preferences[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
                sleepTimerMinutes = preferences[Keys.SLEEP_TIMER_MINUTES] ?: 0,
                sleepTimerFinishCurrentSong = preferences[Keys.SLEEP_FINISH_SONG] ?: false
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR_HEX] = hex }
    }

    suspend fun setActiveQueueId(queueId: Int) {
        context.dataStore.edit { it[Keys.ACTIVE_QUEUE_ID] = queueId }
    }

    suspend fun setPauseOnHeadset(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_HEADSET] = enabled }
    }

    suspend fun setResumeOnHeadset(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_HEADSET] = enabled }
    }

    suspend fun setShuffleMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_MODE] = enabled }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode }
    }

    suspend fun toggleExcludedFolder(folderPath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet()
            if (current.contains(folderPath)) {
                prefs[Keys.EXCLUDED_FOLDERS] = current - folderPath
            } else {
                prefs[Keys.EXCLUDED_FOLDERS] = current + folderPath
            }
        }
    }
}
