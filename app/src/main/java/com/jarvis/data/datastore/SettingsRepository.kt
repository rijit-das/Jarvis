package com.jarvis.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jarvis_settings"
)

/**
 * Non-sensitive user settings (DataStore). Secrets live in
 * `SecureSettingsRepository` (EncryptedSharedPreferences).
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val ONLINE_MODE_ENABLED = booleanPreferencesKey("online_mode_enabled")
        val ONLINE_CONSENT_GIVEN = booleanPreferencesKey("online_consent_given")
        val ONLINE_BASE_URL = stringPreferencesKey("online_base_url")
        val ONLINE_MODEL = stringPreferencesKey("online_model")
        val VOICE_OUTPUT_ENABLED = booleanPreferencesKey("voice_output_enabled")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val CLAP_ENABLED = booleanPreferencesKey("clap_enabled")
        val CLAP_THRESHOLD = floatPreferencesKey("clap_threshold")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val AUDIT_ENABLED = booleanPreferencesKey("audit_enabled")
        val LOCALE_TAG = stringPreferencesKey("locale_tag")
    }

    val offlineMode: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.OFFLINE_MODE] ?: true }
    val onlineModeEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.ONLINE_MODE_ENABLED] ?: false }
    val onlineConsentGiven: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.ONLINE_CONSENT_GIVEN] ?: false }
    val onlineBaseUrl: Flow<String> = context.settingsDataStore.data.map { it[Keys.ONLINE_BASE_URL] ?: "https://api.openai.com/v1" }
    val onlineModel: Flow<String> = context.settingsDataStore.data.map { it[Keys.ONLINE_MODEL] ?: "gpt-4o-mini" }
    val voiceOutputEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.VOICE_OUTPUT_ENABLED] ?: true }
    val ttsRate: Flow<Float> = context.settingsDataStore.data.map { it[Keys.TTS_RATE] ?: 1.0f }
    val ttsPitch: Flow<Float> = context.settingsDataStore.data.map { it[Keys.TTS_PITCH] ?: 1.0f }
    val wakeWordEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.WAKE_WORD_ENABLED] ?: false }
    val clapEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.CLAP_ENABLED] ?: false }
    val clapThreshold: Flow<Float> = context.settingsDataStore.data.map { it[Keys.CLAP_THRESHOLD] ?: 0.35f }
    val memoryEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.MEMORY_ENABLED] ?: true }
    val auditEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.AUDIT_ENABLED] ?: true }
    val localeTag: Flow<String> = context.settingsDataStore.data.map { it[Keys.LOCALE_TAG] ?: "en-US" }

    suspend fun setOfflineMode(value: Boolean) { context.settingsDataStore.edit { it[Keys.OFFLINE_MODE] = value } }
    suspend fun setOnlineModeEnabled(value: Boolean) { context.settingsDataStore.edit { it[Keys.ONLINE_MODE_ENABLED] = value } }
    suspend fun setOnlineConsentGiven(value: Boolean) { context.settingsDataStore.edit { it[Keys.ONLINE_CONSENT_GIVEN] = value } }
    suspend fun setOnlineBaseUrl(value: String) { context.settingsDataStore.edit { it[Keys.ONLINE_BASE_URL] = value } }
    suspend fun setOnlineModel(value: String) { context.settingsDataStore.edit { it[Keys.ONLINE_MODEL] = value } }
    suspend fun setVoiceOutputEnabled(value: Boolean) { context.settingsDataStore.edit { it[Keys.VOICE_OUTPUT_ENABLED] = value } }
    suspend fun setTtsRate(value: Float) { context.settingsDataStore.edit { it[Keys.TTS_RATE] = value } }
    suspend fun setTtsPitch(value: Float) { context.settingsDataStore.edit { it[Keys.TTS_PITCH] = value } }
    suspend fun setWakeWordEnabled(value: Boolean) { context.settingsDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = value } }
    suspend fun setClapEnabled(value: Boolean) { context.settingsDataStore.edit { it[Keys.CLAP_ENABLED] = value } }
    suspend fun setClapThreshold(value: Float) { context.settingsDataStore.edit { it[Keys.CLAP_THRESHOLD] = value } }
    suspend fun setMemoryEnabled(value: Boolean) { context.settingsDataStore.edit { it[Keys.MEMORY_ENABLED] = value } }
    suspend fun setAuditEnabled(value: Boolean) { context.settingsDataStore.edit { it[Keys.AUDIT_ENABLED] = value } }
    suspend fun setLocaleTag(value: String) { context.settingsDataStore.edit { it[Keys.LOCALE_TAG] = value } }
}
