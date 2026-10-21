package com.cyberkryption.vitals

import android.content.Context
import com.cyberkryption.vitals.data.BackupManager
import com.cyberkryption.vitals.data.PhotoStore
import com.cyberkryption.vitals.data.ReadingRepository
import com.cyberkryption.vitals.data.SettingsRepository
import com.cyberkryption.vitals.data.VitalsDatabase
import com.cyberkryption.vitals.data.settingsDataStore
import com.cyberkryption.vitals.export.Exporter
import com.cyberkryption.vitals.extraction.ExifReader
import com.cyberkryption.vitals.extraction.TextExtractor
import com.cyberkryption.vitals.review.ReviewSessions
import com.cyberkryption.vitals.security.AppLock
import com.cyberkryption.vitals.security.Biometrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/** Manual dependency graph, created once by [VitalsApplication]. */
class AppContainer(context: Context, val clock: Clock = Clock.systemDefaultZone()) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val photos = PhotoStore(appContext)
    val readings = ReadingRepository(VitalsDatabase.build(appContext).readingDao(), photos)
    val settings = SettingsRepository(appContext.settingsDataStore)
    val extractor = TextExtractor(appContext)
    val exif = ExifReader(appContext)
    val reviewSessions = ReviewSessions()
    val exporter = Exporter(appContext, readings)
    val backup = BackupManager(appContext, readings, photos, clock)
    val canAuthenticate: () -> Boolean = { Biometrics.canAuthenticate(appContext) }
    val appLock = AppLock(settings, applicationScope, canAuthenticate)
}
