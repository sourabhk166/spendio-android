package com.spendio.app.capture

import javax.inject.Qualifier

/** Qualifier for the boolean flag that gates debug-only raw notification logging. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CollectRawSamples
