package com.strummer.practice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val tempoBpm: Int = 80,
    val lastSongTitle: String = "",
    val lastSectionName: String = "",
    val lastPatternId: String = "",
    val rampEnabled: Boolean = false,
    val rampStartBpm: Int = 60,
    val rampEndBpm: Int = 100,
    val rampIncrement: Int = 2,
    val barsPerIncrement: Int = 4,
    val customPatternDsl: String = "D D U U D U",
    val customChordSequence: String = "A C D G"
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val TempoBpm = intPreferencesKey("tempo_bpm")
        val LastSongTitle = stringPreferencesKey("last_song_title")
        val LastSectionName = stringPreferencesKey("last_section_name")
        val LastPatternId = stringPreferencesKey("last_pattern_id")

        val RampEnabled = booleanPreferencesKey("ramp_enabled")
        val RampStartBpm = intPreferencesKey("ramp_start_bpm")
        val RampEndBpm = intPreferencesKey("ramp_end_bpm")
        val RampIncrement = intPreferencesKey("ramp_increment")
        val BarsPerIncrement = intPreferencesKey("bars_per_increment")

        val CustomPatternDsl = stringPreferencesKey("custom_pattern_dsl")
        val CustomChordSequence = stringPreferencesKey("custom_chord_sequence")
    }

    val settingsFlow: Flow<UserSettings> = context.userPrefsDataStore.data.map { prefs ->
        UserSettings(
            tempoBpm = prefs[Keys.TempoBpm] ?: 80,
            lastSongTitle = prefs[Keys.LastSongTitle] ?: "",
            lastSectionName = prefs[Keys.LastSectionName] ?: "",
            lastPatternId = prefs[Keys.LastPatternId] ?: "",
            rampEnabled = prefs[Keys.RampEnabled] ?: false,
            rampStartBpm = prefs[Keys.RampStartBpm] ?: 60,
            rampEndBpm = prefs[Keys.RampEndBpm] ?: 100,
            rampIncrement = prefs[Keys.RampIncrement] ?: 2,
            barsPerIncrement = prefs[Keys.BarsPerIncrement] ?: 4,
            customPatternDsl = prefs[Keys.CustomPatternDsl] ?: "D D U U D U",
            customChordSequence = prefs[Keys.CustomChordSequence] ?: "A C D G"
        )
    }

    suspend fun updateTempo(bpm: Int) {
        context.userPrefsDataStore.edit { it[Keys.TempoBpm] = bpm }
    }

    suspend fun updateSongSelection(title: String, section: String) {
        context.userPrefsDataStore.edit {
            it[Keys.LastSongTitle] = title
            it[Keys.LastSectionName] = section
        }
    }

    suspend fun updatePatternSelection(patternId: String) {
        context.userPrefsDataStore.edit { it[Keys.LastPatternId] = patternId }
    }

    suspend fun updateRamp(enabled: Boolean, start: Int, end: Int, increment: Int, bars: Int) {
        context.userPrefsDataStore.edit {
            it[Keys.RampEnabled] = enabled
            it[Keys.RampStartBpm] = start
            it[Keys.RampEndBpm] = end
            it[Keys.RampIncrement] = increment
            it[Keys.BarsPerIncrement] = bars
        }
    }

    suspend fun updateCustomPractice(sequence: String, patternDsl: String) {
        context.userPrefsDataStore.edit {
            it[Keys.CustomChordSequence] = sequence
            it[Keys.CustomPatternDsl] = patternDsl
        }
    }
}
