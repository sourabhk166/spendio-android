package com.khaata.app.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {

    private val plaintext = "the quick brown fox — ₹1,234.56 📈".toByteArray(Charsets.UTF_8)

    @Test fun `encrypt then decrypt round-trips with the right passphrase`() {
        val blob = BackupCrypto.encrypt(plaintext, "correct horse battery".toCharArray())
        val out = BackupCrypto.decrypt(blob, "correct horse battery".toCharArray())
        assertArrayEquals(plaintext, out)
    }

    @Test fun `wrong passphrase fails`() {
        val blob = BackupCrypto.encrypt(plaintext, "right-one".toCharArray())
        assertThrows(BackupCrypto.BadPassphraseOrFormat::class.java) {
            BackupCrypto.decrypt(blob, "wrong-one".toCharArray())
        }
    }

    @Test fun `non-khaata bytes are rejected`() {
        assertThrows(BackupCrypto.BadPassphraseOrFormat::class.java) {
            BackupCrypto.decrypt("just some random file".toByteArray(), "whatever".toCharArray())
        }
    }

    @Test fun `each encryption uses a fresh salt and iv`() {
        val a = BackupCrypto.encrypt(plaintext, "pass".toCharArray())
        val b = BackupCrypto.encrypt(plaintext, "pass".toCharArray())
        // Same input + passphrase must still produce different ciphertext (random salt/IV).
        assert(!a.contentEquals(b))
    }
}
