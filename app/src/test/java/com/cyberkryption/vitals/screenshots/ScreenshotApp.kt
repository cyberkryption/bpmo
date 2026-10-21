package com.cyberkryption.vitals.screenshots

import com.cyberkryption.vitals.AppContainer
import com.cyberkryption.vitals.VitalsApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone

/** The app on a fixed evening, Tuesday 20 October 2026, in UTC, so screenshots don't drift. */
class ScreenshotApp : VitalsApplication() {
    override fun onCreate() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        super.onCreate()
    }

    override fun createContainer(): AppContainer = AppContainer(this, Clock.fixed(NOW, ZoneOffset.UTC))

    companion object {
        val NOW: Instant = Instant.parse("2026-10-20T18:00:00Z")
    }
}
