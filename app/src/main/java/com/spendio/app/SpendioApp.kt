package com.spendio.app

import android.app.Application
import com.spendio.app.data.backup.AutoBackupController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Annotated with [HiltAndroidApp] to bootstrap
 * the Hilt dependency graph.
 */
@HiltAndroidApp
class SpendioApp : Application() {

    @Inject lateinit var autoBackupController: AutoBackupController

    override fun onCreate() {
        super.onCreate()
        // Watch for data changes and keep the encrypted auto-backup blob fresh (if enabled).
        autoBackupController.start()
    }
}
