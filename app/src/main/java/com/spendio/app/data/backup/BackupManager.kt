package com.spendio.app.data.backup

import com.spendio.app.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates and restores encrypted backups. A backup is the full transaction list, serialized to
 * JSON and encrypted with the user's passphrase (see [BackupCrypto]). Restore is idempotent:
 * transactions are inserted via the repository's dedupe, so importing the same file twice — or
 * merging a backup into a non-empty database — never creates duplicates.
 */
@Singleton
class BackupManager @Inject constructor(
    private val repository: TransactionRepository,
) {
    /** Builds the encrypted backup bytes for every stored transaction. */
    suspend fun createBackup(passphrase: CharArray): ByteArray {
        val json = BackupSerializer.encode(repository.allTransactions())
        return BackupCrypto.encrypt(json.toByteArray(Charsets.UTF_8), passphrase)
    }

    /**
     * Decrypts and imports [blob], returning how many *new* transactions were added.
     * Throws [BackupCrypto.BadPassphraseOrFormat] on a wrong passphrase or unreadable file.
     */
    suspend fun restoreBackup(blob: ByteArray, passphrase: CharArray): Int {
        val json = String(BackupCrypto.decrypt(blob, passphrase), Charsets.UTF_8)
        val transactions = BackupSerializer.decode(json)
        var added = 0
        for (txn in transactions) {
            if (repository.insertIfNew(txn)) added++
        }
        return added
    }
}
