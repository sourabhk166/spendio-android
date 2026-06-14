package com.khaata.app.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and persists the SQLCipher passphrase for the local database. The 256-bit
 * random passphrase is itself encrypted with an AES key held in the **Android Keystore**
 * (hardware-backed where available) and stored as ciphertext in private prefs — so the
 * passphrase never exists in plaintext at rest. See ADR-006.
 */
@Singleton
class DatabaseKey @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Returns the stable DB passphrase, creating + sealing it on first use. */
    fun getOrCreatePassphrase(): ByteArray {
        val storedCt = prefs.getString(KEY_CIPHERTEXT, null)
        val storedIv = prefs.getString(KEY_IV, null)
        if (storedCt != null && storedIv != null) {
            return decrypt(Base64.decode(storedCt, Base64.NO_WRAP), Base64.decode(storedIv, Base64.NO_WRAP))
        }
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val (ct, iv) = encrypt(passphrase)
        prefs.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ct, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        return passphrase
    }

    private fun encrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return cipher.doFinal(plain) to cipher.iv
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFS = "khaata_secure"
        private const val KEY_CIPHERTEXT = "db_pass_ct"
        private const val KEY_IV = "db_pass_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "khaata_db_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
