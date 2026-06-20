package com.spendio.app.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the user's *backup* passphrase on-device, sealed by an Android-Keystore AES key
 * (hardware-backed where available), so the automatic backup blob can be regenerated in the
 * background without prompting. Mirrors [com.spendio.app.data.db.DatabaseKey]'s sealing.
 *
 * Crucially this seal is device-local and is NOT part of Auto Backup — on a new device the
 * Keystore key doesn't exist, so the cached passphrase can't be recovered there. That's by
 * design: restoring on a new device requires the user to type the passphrase, keeping the
 * backup truly end-to-end encrypted.
 */
@Singleton
class AutoBackupSecret @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPassphrase(): Boolean = prefs.contains(KEY_CT) && prefs.contains(KEY_IV)

    fun setPassphrase(passphrase: CharArray) {
        val (ct, iv) = encrypt(String(passphrase).toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_CT, Base64.encodeToString(ct, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    /** The cached passphrase, or null if none is stored. */
    fun getPassphrase(): CharArray? {
        val ct = prefs.getString(KEY_CT, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val plain = decrypt(Base64.decode(ct, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP))
        return String(plain, Charsets.UTF_8).toCharArray()
    }

    fun clear() {
        prefs.edit().remove(KEY_CT).remove(KEY_IV).apply()
    }

    private fun encrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return cipher.doFinal(plain) to cipher.iv
    }

    private fun decrypt(ct: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
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
        private const val PREFS = "spendio_autobackup_secret"
        private const val KEY_CT = "pass_ct"
        private const val KEY_IV = "pass_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "spendio_autobackup_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
