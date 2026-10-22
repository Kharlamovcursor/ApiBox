package ru.hdlabs.apibox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class AppSettings(
    val tiles: Boolean = true,
    val animationEnabled: Boolean = true,
    val animationSpeedPercent: Int = 100,
    val powerSave: Boolean = false,
)

private val Context.settingsDataStore by preferencesDataStore(name = "apibox_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = store.data
        .map { prefs ->
            AppSettings(
                tiles = prefs[TILES] ?: true,
                animationEnabled = prefs[ANIMATION] ?: true,
                animationSpeedPercent = prefs[SPEED] ?: 100,
                powerSave = prefs[POWER] ?: false,
            )
        }
        .catch { emit(AppSettings()) }

    suspend fun setTiles(value: Boolean) {
        store.edit { it[TILES] = value }
    }

    suspend fun setAnimationEnabled(value: Boolean) {
        store.edit { it[ANIMATION] = value }
    }

    suspend fun setAnimationSpeed(percent: Int) {
        store.edit { it[SPEED] = percent.coerceIn(40, 160) }
    }

    suspend fun setPowerSave(value: Boolean) {
        store.edit { it[POWER] = value }
    }

    private companion object {
        val TILES = booleanPreferencesKey("tiles")
        val ANIMATION = booleanPreferencesKey("animation")
        val SPEED = intPreferencesKey("speed")
        val POWER = booleanPreferencesKey("power")
    }
}
