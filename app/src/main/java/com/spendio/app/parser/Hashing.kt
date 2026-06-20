package com.spendio.app.parser

import java.security.MessageDigest

/** SHA-256 hex digest. Used for dedupe keys and for storing a hash of raw text
 *  instead of the text itself (privacy — see docs/04-lld.md §2). */
fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
