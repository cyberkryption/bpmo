package com.cyberkryption.vitals.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.cyberkryption.vitals.core.Guideline
import com.cyberkryption.vitals.core.Period
import com.cyberkryption.vitals.data.BpChart
import com.cyberkryption.vitals.data.RelockTimeout
import com.cyberkryption.vitals.export.HealthReport
import com.cyberkryption.vitals.review.ReviewItem
import com.cyberkryption.vitals.ui.VitalsApp
import com.cyberkryption.vitals.ui.lock.LockContent
import com.cyberkryption.vitals.ui.lock.LockStatus
import com.cyberkryption.vitals.ui.navigation.CaptureRoute
import com.cyberkryption.vitals.ui.navigation.HistoryRoute
import com.cyberkryption.vitals.ui.navigation.ReadingRoute
import com.cyberkryption.vitals.ui.navigation.ReviewRoute
import com.cyberkryption.vitals.ui.navigation.SettingsRoute
import com.cyberkryption.vitals.ui.navigation.TrendsRoute
import com.cyberkryption.vitals.ui.theme.VitalsTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

private const val PHONE = "w412dp-h915dp-xhdpi"
private const val FOLDABLE = "w720dp-h900dp-xhdpi"
private const val TABLET = "w1280dp-h800dp-hdpi"

/**
 * Renders the real screens, with sample data, into docs/screenshots for the README.
 * Run with: ./gradlew :app:testDebugUnitTest -Pscreenshots
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = ScreenshotApp::class, qualifiers = PHONE)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: ScreenshotApp get() = ApplicationProvider.getApplicationContext()
    private val outDir = File(System.getProperty("vitals.screenshots.dir") ?: "build/screenshots")
    private var featured = 0L

    @Before
    fun seed() {
        featured = runBlocking { SampleData.seed(app.container, File(app.filesDir, "sample")) }
    }

    @Test
    fun phoneLight() = phoneTour(dark = false)

    @Test
    fun phoneDark() = phoneTour(dark = true)

    @Test
    @Config(qualifiers = TABLET)
    fun tabletLight() = tablet(dark = false)

    @Test
    @Config(qualifiers = TABLET)
    fun tabletDark() = tablet(dark = true)

    @Test
    @Config(qualifiers = FOLDABLE)
    fun foldable() {
        val nav = launch(dark = false)
        waitForDashboard()
        go { nav.navigate(HistoryRoute) }
        waitFor(hasText("Today ·", substring = true))
        compose.onAllNodes(hasText("8:42 AM") and hasClickAction())[0].performClick()
        waitFor(hasText("Date source"))
        settle()
        shoot("light-foldable")
    }

    @Test
    fun lockLight() = lock(dark = false)

    @Test
    fun lockDark() = lock(dark = true)

    /**
     * Every page of a month's PDF report, drawn as the PDF draws it, at twice its size in points,
     * and the pages that change when the blood pressure chart is drawn as lines.
     */
    @Test
    fun report() {
        val clock = app.container.clock
        val today = LocalDate.now(clock)
        val period = Period(today.minusDays(29), today)
        val readings = runBlocking { app.container.readings.between(period.startInstant(clock.zone), period.endInstant(clock.zone)) }
        fun render(chart: BpChart) = HealthReport(app, clock.zone).pages(period, readings, Guideline.AHA, chart, today).map { page ->
            Bitmap.createBitmap(595 * 2, 842 * 2, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply {
                    scale(2f, 2f)
                    page(this)
                }
            }
        }
        val bars = render(BpChart.BARS)
        val lines = render(BpChart.LINES)
        fun save(bitmap: Bitmap, name: String) =
            bitmap.captureRoboImage(File(outDir, "$name.png").path, RoborazziOptions(taskType = RoborazziTaskType.Record))
        bars.forEachIndexed { i, page -> save(page, "report-${i + 1}") }
        lines.forEachIndexed { i, page -> if (bars.getOrNull(i)?.sameAs(page) != true) save(page, "report-lines-${i + 1}") }
    }

    @Test
    fun capture() {
        val nav = launch(dark = false)
        waitForDashboard()
        go { nav.navigate(CaptureRoute) }
        waitFor(hasText("Allow camera"))
        shoot("dark-capture")
    }

    private fun phoneTour(dark: Boolean) {
        val mode = if (dark) "dark" else "light"
        val nav = launch(dark)
        waitForDashboard()
        shoot("$mode-dashboard")

        go { nav.navigate(HistoryRoute) }
        waitFor(hasText("Today ·", substring = true))
        shoot("$mode-history")

        go { nav.navigate(TrendsRoute) }
        waitFor(hasText("Summary"))
        shoot("$mode-trends")
        if (!dark) {
            compose.onNode(hasText("Lines") and hasClickAction()).performClick()
            compose.waitUntil(timeoutMillis = 15_000) { compose.onAllNodes(hasText("Bar =", substring = true)).fetchSemanticsNodes().isEmpty() }
            compose.waitForIdle()
            shoot("light-trends-lines")
            compose.onNode(hasText("Bars") and hasClickAction()).performClick()
            compose.waitForIdle()
        }

        go { nav.navigate(ReadingRoute(featured)) }
        waitFor(hasText("Date source"))
        settle()
        shoot("$mode-detail")

        val reading = runBlocking { app.container.readings.get(featured) }!!
        val session = app.container.reviewSessions.create(
            listOf(ReviewItem(Uri.parse(reading.photoUri), reading.takenAt, reading.takenAtSource, existing = reading)),
        )
        go { nav.navigate(ReviewRoute(session)) }
        waitFor(hasText("Category"))
        settle()
        shoot("$mode-review")

        go { nav.navigate(SettingsRoute) }
        waitFor(hasText("Require biometric to open"))
        shoot("$mode-settings")
    }

    private fun tablet(dark: Boolean) {
        launch(dark)
        waitForDashboard()
        waitFor(hasText("Summary"))
        shoot(if (dark) "dark-tablet" else "light-tablet")
    }

    private fun lock(dark: Boolean) {
        compose.setContent {
            VitalsTheme(darkTheme = dark) {
                LockContent(LockStatus.Idle, attemptsLeft = 3, relockTimeout = RelockTimeout.ONE_MINUTE, onSensor = {}, onUseCredential = {})
            }
        }
        waitFor(hasText("Vitals locked"))
        shoot(if (dark) "dark-lock" else "light-lock")
    }

    private fun launch(dark: Boolean): NavHostController {
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            VitalsTheme(darkTheme = dark) { VitalsApp(app.container, nav) }
        }
        compose.waitForIdle()
        return nav
    }

    private fun waitForDashboard() = waitFor(hasContentDescription("over", substring = true))

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(timeoutMillis = 15_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun go(action: () -> Unit) {
        compose.runOnUiThread(action)
        compose.waitForIdle()
    }

    /** Gives photos a moment to decode off the main thread. */
    private fun settle() {
        repeat(5) {
            Thread.sleep(200)
            compose.waitForIdle()
        }
    }

    private fun shoot(name: String) {
        compose.onRoot().captureRoboImage(
            filePath = File(outDir, "$name.png").path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
