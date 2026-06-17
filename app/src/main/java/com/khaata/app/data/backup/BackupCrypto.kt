package com.khaata.app.data.backup

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts the backup payload with a key derived from the user's passphrase (PBKDF2-HMAC-SHA256)
 * and AES-256-GCM. The passphrase — not any device key — protects the file, so the backup is
 * restorable on a *new* device (the whole point) yet unreadable to anyone, including us. This is
 * separate from the live DB's SQLCipher key, which is never touched.
 *
 * File layout:  [MAGIC(5)] [salt(16)] [iv(12)] [ciphertext+GCM tag]
 */
object BackupCrypto {
    private val MAGIC = "KHBK1".toByteArray(Charsets.US_ASCII) // Khaata BacKup v1
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    /** Thrown when the passphrase is wrong or the file is corrupt / not a Khaata backup. */
    class BadPassphraseOrFormat(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val iv = cipher.iv // GCM default 12-byte random IV
        val ciphertext = cipher.doFinal(plaintext)
        return MAGIC + salt + iv + ciphertext
    }

    fun decrypt(blob: ByteArray, passphrase: CharArray): ByteArray {
        val headerLen = MAGIC.size + SALT_LEN + IV_LEN
        if (blob.size <= headerLen || !blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BadPassphraseOrFormat("Not a Khaata backup file")
        }
        val salt = blob.copyOfRange(MAGIC.size, MAGIC.size + SALT_LEN)
        val iv = blob.copyOfRange(MAGIC.size + SALT_LEN, headerLen)
        val ciphertext = blob.copyOfRange(headerLen, blob.size)
        val key = deriveKey(passphrase, salt)
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                doFinal(ciphertext)
            }
        } catch (e: AEADBadTagException) {
            throw BadPassphraseOrFormat("Wrong passphrase", e)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
