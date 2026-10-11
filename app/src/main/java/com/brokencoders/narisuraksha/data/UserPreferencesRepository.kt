package com.brokencoders.narisuraksha.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DECOY_MODE_ENABLED = booleanPreferencesKey("decoy_mode_enabled")
        val SECRET_DECOY_CODE = stringPreferencesKey("secret_decoy_code")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHAKE_DETECTION_ENABLED = booleanPreferencesKey("shake_detection_enabled")
        val GUARDIAN_SCAN_ENABLED = booleanPreferencesKey("guardian_scan_enabled")
    }

    val isDecoyModeEnabled: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
        preferences[PreferencesKeys.DECOY_MODE_ENABLED] ?: false
    }

    val secretDecoyCode: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
        preferences[PreferencesKeys.SECRET_DECOY_CODE] ?: "1234"
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val isShakeDetectionEnabled: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
        preferences[PreferencesKeys.SHAKE_DETECTION_ENABLED] ?: true
    }

    val isGuardianScanEnabled: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
        preferences[PreferencesKeys.GUARDIAN_SCAN_ENABLED] ?: true
    }

    suspend fun setDecoyModeEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DECOY_MODE_ENABLED] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist decoy mode", e)
        }
    }

    suspend fun setSecretDecoyCode(code: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SECRET_DECOY_CODE] = code
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist decoy code", e)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist onboarding state", e)
        }
    }

    suspend fun setShakeDetectionEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHAKE_DETECTION_ENABLED] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist shake setting", e)
        }
    }

    suspend fun setGuardianScanEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GUARDIAN_SCAN_ENABLED] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist guardian scan setting", e)
        }
    }

    companion object {
        private const val TAG = "UserPrefs"
    }
}
