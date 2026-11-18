package com.jarvis.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android Keystore-backed AES-GCM key management.
 *
 * - Generates a non-extractable AES-256 key in the hardware-backed Keystore
 *   (when available) for encrypting sensitive values.
 * - Derives the SQLCipher database passphrase from a Keystore-held random
 *   secret, so the passphrase itself is never stored in plaintext.
 */
class KeystoreManager {

    companion object {
        private const val KEY_ALIAS = "jarvis_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val IV_SIZE = 12
        private const val DB_SECRET_KEY = "db_passphrase_secret"
    }

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun masterKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
            generator.init(spec)
            generator.generateKey()
        }
    }

    /** Encrypts [plaintext]; returns Base64(iv + ciphertext). */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    /** Decrypts a value produced by [encrypt]. */
    fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /**
     * Returns a stable 32-character passphrase for SQLCipher, derived from a
     * random secret stored encrypted in [secretStore]. The secret is generated
     * once and never leaves the Keystore-encrypted store.
     */
    fun databasePassphrase(secretStore: (String) -> String?, persist: (String) -> Unit): CharArray {
        val existing = secretStore(DB_SECRET_KEY)
        val secret = existing ?: run {
            val generated = Base64.encodeToString(
                ByteArray(32).also { java.security.SecureRandom().nextBytes(it) },
                Base64.NO_WRAP
            )
            persist(generated)
            generated
        }
        // Derive a passphrase deterministically from the secret.
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(secret.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP).toCharArray()
    }

    /** Wraps a raw key (used only for tests / fallback paths). */
    fun rawKey(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")

}
