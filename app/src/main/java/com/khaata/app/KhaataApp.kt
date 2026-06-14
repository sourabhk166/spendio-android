package com.khaata.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotated with [HiltAndroidApp] to bootstrap
 * the Hilt dependency graph.
 */
@HiltAndroidApp
class KhaataApp : Application()
