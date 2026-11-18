package com.jarvis.data.datastore

import com.jarvis.core.security.SecurePreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secrets storage facade. The online-provider API key lives here, encrypted
 * with a Keystore-held key. Values are never logged.
 */
@Singleton
class SecureSettingsRepository @Inject constructor(
    private val securePreferences: SecurePreferences
) {
    companion object {
        const val KEY_API_KEY = "online_api_key"
        const val KEY_DB_SECRET = "db_passphrase_secret"
    }

    fun getApiKey(): String? = securePreferences.getString(KEY_API_KEY)?.takeIf { it.isNotBlank() }

    fun setApiKey(key: String) {
        if (key.isBlank()) securePreferences.remove(KEY_API_KEY)
        else securePreferences.putString(KEY_API_KEY, key.trim())
    }

    fun clearApiKey() = securePreferences.remove(KEY_API_KEY)

    fun getDbSecret(): String? = securePreferences.getString(KEY_DB_SECRET)

    fun setDbSecret(secret: String) = securePreferences.putString(KEY_DB_SECRET, secret)
}
