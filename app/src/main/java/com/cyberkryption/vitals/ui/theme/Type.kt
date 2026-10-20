package com.cyberkryption.vitals.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cyberkryption.vitals.R

val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

val BarlowCondensed = FontFamily(Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold))

private const val TABULAR = "tnum"

/** Type roles from the handoff. Kickers and tags are uppercased at the call site. */
object VitalsType {
    private val heading = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold)
    private val body = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal)

    val hero = heading.copy(fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.02).em, fontFeatureSettings = TABULAR)
    val detailValue = heading.copy(fontSize = 72.sp, lineHeight = 72.sp, letterSpacing = (-0.02).em, fontFeatureSettings = TABULAR)
    val tileValue = heading.copy(fontSize = 44.sp, lineHeight = 44.sp, fontFeatureSettings = TABULAR)
    val screenTitle = heading.copy(fontSize = 30.sp, lineHeight = 33.sp)
    val lockTitle = heading.copy(fontSize = 36.sp, lineHeight = 40.sp)
    val appBarTitle = heading.copy(fontSize = 22.sp, lineHeight = 28.sp)
    val section = heading.copy(fontSize = 20.sp, lineHeight = 24.sp)
    val chartValue = heading.copy(fontSize = 28.sp, lineHeight = 32.sp, fontFeatureSettings = TABULAR)
    val stripValue = heading.copy(fontSize = 24.sp, lineHeight = 28.sp, fontFeatureSettings = TABULAR)
    val rowValue = heading.copy(fontSize = 22.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR)
    val inputValue = heading.copy(fontSize = 28.sp, lineHeight = 32.sp, fontFeatureSettings = TABULAR)
    val primaryButton = heading.copy(fontSize = 17.sp, lineHeight = 20.sp, letterSpacing = 0.02.em)
    val button = heading.copy(fontSize = 16.sp, lineHeight = 20.sp)
    val action = heading.copy(fontSize = 15.sp, lineHeight = 20.sp)
    val link = heading.copy(fontSize = 14.sp, lineHeight = 18.sp)
    val stepper = heading.copy(fontSize = 16.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR)
    val status = heading.copy(fontSize = 18.sp, lineHeight = 22.sp)

    val bodyLarge = body.copy(fontSize = 15.sp, lineHeight = 22.sp)
    val bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 21.sp)
    val control = body.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val meta = body.copy(fontSize = 12.sp, lineHeight = 17.sp)
    val metaNumbers = meta.copy(fontFeatureSettings = TABULAR)
    val caption = body.copy(fontSize = 11.sp, lineHeight = 15.sp)
    val kicker = body.copy(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.1.em)
    val overline = body.copy(fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.08.em)
    val tag = body.copy(fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.06.em)
    val tagLarge = body.copy(fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em)
    val axis = body.copy(fontSize = 10.sp, lineHeight = 12.sp, fontFeatureSettings = TABULAR)
    val navLabel = body.copy(fontSize = 12.sp, lineHeight = 16.sp)
}

/** M3 slots mapped onto the roles so dialogs, menus and pickers match. */
val VitalsTypography = Typography(
    displayLarge = VitalsType.detailValue,
    displayMedium = VitalsType.hero,
    displaySmall = VitalsType.tileValue,
    headlineLarge = VitalsType.lockTitle,
    headlineMedium = VitalsType.screenTitle,
    headlineSmall = VitalsType.inputValue,
    titleLarge = VitalsType.appBarTitle,
    titleMedium = VitalsType.section,
    titleSmall = VitalsType.button,
    bodyLarge = VitalsType.bodyLarge,
    bodyMedium = VitalsType.bodyMedium,
    bodySmall = VitalsType.meta,
    labelLarge = VitalsType.button,
    labelMedium = VitalsType.meta,
    labelSmall = VitalsType.caption,
)
