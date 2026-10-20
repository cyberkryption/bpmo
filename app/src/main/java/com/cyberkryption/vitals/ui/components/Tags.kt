package com.cyberkryption.vitals.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Status
import com.cyberkryption.vitals.ui.theme.ToneColors
import com.cyberkryption.vitals.ui.theme.Vitals
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

@Composable
fun StatusTag(status: Status, modifier: Modifier = Modifier, long: Boolean = false, large: Boolean = false) {
    Tag(
        text = stringResource(if (long) status.longLabel() else status.shortLabel()),
        tone = Vitals.colors.tone(status.tone),
        modifier = modifier,
        large = large || long,
    )
}

/** Uppercase label on a tinted fill with a square dot. */
@Composable
fun Tag(text: String, tone: ToneColors, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(
        modifier = modifier
            .background(tone.fill)
            .padding(horizontal = if (large) 8.dp else 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(tone.base))
        Text(
            text = text.uppercase(),
            style = if (large) VitalsType.tagLarge else VitalsType.tag,
            color = tone.ink,
            maxLines = 1,
        )
    }
}

/** Small uppercase label above a value or section. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = Vitals.colors.accent700) {
    Text(text = text.uppercase(), style = VitalsType.kicker, color = color, modifier = modifier, maxLines = 1)
}
