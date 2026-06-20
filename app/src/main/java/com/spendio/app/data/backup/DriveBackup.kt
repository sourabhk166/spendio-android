package com.spendio.app.data.backup

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional Google Drive backup of the passphrase-encrypted blob. The file lives in Drive's
 * private **appDataFolder** (hidden from the user's Drive UI, only this app can touch it), so
 * only ciphertext ever leaves the device — Google can't read it, and neither can we.
 *
 * Uses Google Sign-In for the `drive.appdata` scope and the Drive REST API directly (an OAuth
 * token via [GoogleAuthUtil] + HttpURLConnection), deliberately avoiding the heavyweight Drive
 * Java client. Network calls run on IO. Sign-in itself is driven from the UI via [signInClient].
 */
@Singleton
class DriveBackup @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val driveScope = Scope(DRIVE_APPDATA)

    fun signInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** The connected account if signed in AND the Drive scope was granted, else null. */
    fun connectedAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)?.takeIf {
            GoogleSignIn.hasPermissions(it, driveScope)
        }

    fun isConnected(): Boolean = connectedAccount() != null

    val accountEmail: String? get() = connectedAccount()?.email

    /** Uploads [bytes] as the backup file, replacing any existing one. Requires a connected account. */
    suspend fun upload(bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        val token = freshToken()
        val existingId = findBackupId(token)
        if (existingId == null) {
            val id = createMetadata(token)
            uploadMedia(token, id, bytes)
        } else {
            uploadMedia(token, existingId, bytes)
        }
    }

    /** Downloads the backup blob from Drive, or null if there isn't one. */
    suspend fun download(): ByteArray? = withContext(Dispatchers.IO) {
        val token = freshToken()
        val id = findBackupId(token) ?: return@withContext null
        downloadMedia(token, id)
    }

    private fun freshToken(): String {
        val account = connectedAccount()?.account ?: error("Drive not connected")
        return GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_APPDATA")
    }

    private fun findBackupId(token: String): String? {
        val q = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id,name)"
        val body = request(url, "GET", token)
        val files = JSONObject(body).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createMetadata(token: String): String {
        val metadata = JSONObject()
            .put("name", FILE_NAME)
            .put("parents", listOf("appDataFolder").let { org.json.JSONArray(it) })
            .toString()
        val body = request(
            urlString = "https://www.googleapis.com/drive/v3/files?fields=id",
            method = "POST",
            token = token,
            contentType = "application/json; charset=UTF-8",
            payload = metadata.toByteArray(Charsets.UTF_8),
        )
        return JSONObject(body).getString("id")
    }

    private fun uploadMedia(token: String, fileId: String, bytes: ByteArray) {
        request(
            urlString = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media",
            method = "PATCH",
            token = token,
            contentType = "application/octet-stream",
            payload = bytes,
        )
    }

    private fun downloadMedia(token: String, fileId: String): ByteArray {
        val conn = (URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media").openConnection() as HttpURLConnection)
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        try {
            check(conn.responseCode in 200..299) { "Drive download failed: ${conn.responseCode}" }
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun request(
        urlString: String,
        method: String,
        token: String,
        contentType: String? = null,
        payload: ByteArray? = null,
    ): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection)
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (contentType != null) conn.setRequestProperty("Content-Type", contentType)
        if (payload != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(payload) }
        }
        try {
            check(conn.responseCode in 200..299) { "Drive $method failed: ${conn.responseCode}" }
            val out = ByteArrayOutputStream()
            conn.inputStream.use { it.copyTo(out) }
            return out.toString("UTF-8")
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val FILE_NAME = "spendio-backup.kbk"
    }
}
