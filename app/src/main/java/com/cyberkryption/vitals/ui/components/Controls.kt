package com.cyberkryption.vitals.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsShape
import com.cyberkryption.vitals.ui.theme.VitalsType

@Immutable
data class SegmentedColors(
    val track: Color,
    val selected: Color,
    val selectedText: Color,
    val text: Color,
)

@Composable
fun segmentedColors(): SegmentedColors {
    val c = Vitals.colors
    return SegmentedColors(track = c.surfaceVariant, selected = c.primary, selectedText = c.onPrimary, text = c.text)
}

/** Pill track; the selected option slides into a filled pill. */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    fillWidth: Boolean = false,
    colors: SegmentedColors = segmentedColors(),
) {
    Row(
        modifier = modifier
            .clip(VitalsShape.pill)
            .background(colors.track)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val fill by animateColorAsState(if (isSelected) colors.selected else Color.Transparent, label = "segment")
            Box(
                modifier = Modifier
                    .then(if (fillWidth) Modifier.weight(1f) else Modifier)
                    .clip(VitalsShape.pill)
                    .background(fill)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(value) })
                    .heightIn(min = height - 6.dp)
                    .padding(horizontal = if (fillWidth) 6.dp else 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = VitalsType.control,
                    color = if (isSelected) colors.selectedText else colors.text,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

enum class ChipStyle {
    /** History filters: the selected chip takes the primary fill. */
    Filter,

    /** Note chips: the selected chip gets a tonal fill, an outline and a check. */
    Toggle,
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChipStyle = ChipStyle.Filter,
) {
    val c = Vitals.colors
    val fill = when {
        !selected -> c.surface
        style == ChipStyle.Filter -> c.primary
        else -> c.primaryContainer
    }
    val textColor = when {
        !selected -> c.text
        style == ChipStyle.Filter -> c.onPrimary
        else -> c.onPrimaryContainer
    }
    Row(
        modifier = modifier
            .clip(VitalsShape.pill)
            .background(fill)
            .border(1.dp, if (selected) c.primary else c.border, VitalsShape.pill)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(start = if (selected && style == ChipStyle.Toggle) 10.dp else 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected && style == ChipStyle.Toggle) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
        }
        Text(label, style = VitalsType.control, color = textColor, maxLines = 1)
    }
}

@Composable
fun VitalsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Vitals.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onPrimary,
            checkedTrackColor = c.primary,
            checkedBorderColor = c.primary,
            uncheckedThumbColor = c.muted,
            uncheckedTrackColor = c.surfaceVariant,
            uncheckedBorderColor = c.muted,
        ),
    )
}
