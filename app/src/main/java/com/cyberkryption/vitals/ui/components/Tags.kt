package com.cyberkryption.vitals.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Status
import com.cyberkryption.vitals.ui.theme.ToneColors
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsShape
import com.cyberkryption.vitals.ui.theme.VitalsType

@StringRes
fun Status.shortLabel(): Int = when (this) {
    Status.NORMAL -> R.string.status_normal
    Status.ELEVATED -> R.string.status_elevated
    Status.STAGE1 -> R.string.status_stage1
    Status.STAGE2 -> R.string.status_stage2
    Status.CRISIS -> R.string.status_crisis
    Status.SPO2_NORMAL -> R.string.status_normal
    Status.SPO2_LOW -> R.string.status_spo2_low
    Status.SPO2_VERY_LOW -> R.string.status_spo2_very_low
}

@StringRes
fun Status.longLabel(): Int = when (this) {
    Status.NORMAL -> R.string.status_normal
    Status.ELEVATED -> R.string.status_elevated
    Status.STAGE1 -> R.string.status_stage1_long
    Status.STAGE2 -> R.string.status_stage2_long
    Status.CRISIS -> R.string.status_crisis_long
    Status.SPO2_NORMAL -> R.string.status_spo2_normal_long
    Status.SPO2_LOW -> R.string.status_spo2_low_long
    Status.SPO2_VERY_LOW -> R.string.status_spo2_very_low_long
}

/** The category, always named in words on a tinted pill. */
@Composable
fun StatusBadge(status: Status, modifier: Modifier = Modifier, long: Boolean = false, small: Boolean = false) {
    ToneBadge(
        text = stringResource(if (long) status.longLabel() else status.shortLabel()),
        tone = Vitals.colors.tone(status.tone),
        modifier = modifier,
        small = small,
    )
}

@Composable
fun ToneBadge(text: String, tone: ToneColors, modifier: Modifier = Modifier, small: Boolean = false) {
    val c = Vitals.colors
    Text(
        text = text,
        style = if (small) VitalsType.badgeSmall else VitalsType.badge,
        color = tone.ink,
        maxLines = 1,
        modifier = modifier
            .clip(VitalsShape.pill)
            .background(tone.fill.compositeOver(c.surface))
            .padding(horizontal = if (small) 10.dp else 12.dp, vertical = if (small) 3.dp else 5.dp),
    )
}

/** A small sentence-case label above a group or value. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Vitals.colors.muted) {
    Text(text = text, style = VitalsType.label, color = color, modifier = modifier, maxLines = 1)
}
