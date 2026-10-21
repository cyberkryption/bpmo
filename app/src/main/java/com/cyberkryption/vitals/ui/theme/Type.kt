package com.cyberkryption.vitals.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cyberkryption.vitals.R

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

private const val TABULAR = "tnum"

/** Type roles. Everything is sentence case; numbers that line up use tabular figures. */
object VitalsType {
    private val regular = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal)
    private val medium = regular.copy(fontWeight = FontWeight.Medium)
    private val semibold = regular.copy(fontWeight = FontWeight.SemiBold)

    val hero = semibold.copy(fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-0.01).em, fontFeatureSettings = TABULAR)
    val detailValue = semibold.copy(fontSize = 56.sp, lineHeight = 62.sp, letterSpacing = (-0.01).em, fontFeatureSettings = TABULAR)
    val tileValue = semibold.copy(fontSize = 34.sp, lineHeight = 40.sp, fontFeatureSettings = TABULAR)
    val screenTitle = semibold.copy(fontSize = 30.sp, lineHeight = 36.sp)
    val appTitle = semibold.copy(fontSize = 26.sp, lineHeight = 32.sp)
    val lockTitle = semibold.copy(fontSize = 28.sp, lineHeight = 34.sp)
    val appBarTitle = semibold.copy(fontSize = 20.sp, lineHeight = 26.sp)
    val section = semibold.copy(fontSize = 20.sp, lineHeight = 26.sp)
    val cardTitle = medium.copy(fontSize = 16.sp, lineHeight = 22.sp)
    val chartValue = semibold.copy(fontSize = 26.sp, lineHeight = 32.sp, fontFeatureSettings = TABULAR)
    val stripValue = semibold.copy(fontSize = 22.sp, lineHeight = 28.sp, fontFeatureSettings = TABULAR)
    val inputValue = semibold.copy(fontSize = 26.sp, lineHeight = 32.sp, fontFeatureSettings = TABULAR)
    val rowTitle = medium.copy(fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR)
    val rowSubtitle = regular.copy(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR)
    val button = semibold.copy(fontSize = 16.sp, lineHeight = 20.sp)
    val action = semibold.copy(fontSize = 15.sp, lineHeight = 20.sp)
    val link = semibold.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val stepper = semibold.copy(fontSize = 15.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR)
    val status = semibold.copy(fontSize = 18.sp, lineHeight = 24.sp)

    val subtitle = regular.copy(fontSize = 16.sp, lineHeight = 22.sp)
    val bodyLarge = regular.copy(fontSize = 16.sp, lineHeight = 24.sp)
    val bodyMedium = regular.copy(fontSize = 14.sp, lineHeight = 21.sp)
    val control = medium.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val label = medium.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val meta = regular.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val metaNumbers = meta.copy(fontFeatureSettings = TABULAR)
    val caption = regular.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val overline = medium.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val badge = semibold.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val badgeSmall = semibold.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val axis = regular.copy(fontSize = 11.sp, lineHeight = 14.sp, fontFeatureSettings = TABULAR)
    val navLabel = medium.copy(fontSize = 12.sp, lineHeight = 16.sp)
}

/** M3 slots mapped onto the roles so dialogs, menus and pickers match. */
val VitalsTypography = Typography(
    displayLarge = VitalsType.detailValue,
    displayMedium = VitalsType.hero,
    displaySmall = VitalsType.tileValue,
    headlineLarge = VitalsType.screenTitle,
    headlineMedium = VitalsType.lockTitle,
    headlineSmall = VitalsType.inputValue,
    titleLarge = VitalsType.appBarTitle,
    titleMedium = VitalsType.cardTitle,
    titleSmall = VitalsType.control,
    bodyLarge = VitalsType.bodyLarge,
    bodyMedium = VitalsType.bodyMedium,
    bodySmall = VitalsType.meta,
    labelLarge = VitalsType.button,
    labelMedium = VitalsType.navLabel,
    labelSmall = VitalsType.caption,
)
