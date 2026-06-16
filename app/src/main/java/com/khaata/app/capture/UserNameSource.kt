package com.khaata.app.capture

/**
 * Supplies the user's own name to the capture pipeline for self-transfer detection, without
 * pulling the Android-backed SettingsStore into the otherwise framework-free pipeline (keeps it
 * unit-testable). Provided in DI from SettingsStore; faked in tests.
 */
fun interface UserNameSource {
    suspend fun current(): String
}
