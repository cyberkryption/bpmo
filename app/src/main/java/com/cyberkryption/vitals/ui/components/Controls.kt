package com.cyberkryption.vitals.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

@Immutable
data class SegmentedColors(
    val border: Color,
    val background: Color,
    val selected: Color,
    val selectedText: Color,
    val text: Color,
)

@Composable
fun segmentedColors(): SegmentedColors {
    val c = Vitals.colors
    return SegmentedColors(c.divider, Color.Transparent, c.accent, c.bg, c.text)
}

/** Joined toggle buttons; the selected segment takes the accent fill. */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    fillWidth: Boolean = false,
    colors: SegmentedColors = segmentedColors(),
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(colors.background)
            .border(1.dp, colors.border)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, (value, label) ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .then(if (fillWidth) Modifier.weight(1f) else Modifier)
                    .background(if (isSelected) colors.selected else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(value) })
                    .heightIn(min = height)
                    .padding(horizontal = 16.dp),
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
    /** History filters: selected chip takes the accent fill. */
    Filter,

    /** Note chips: selected chip gets an accent border on an accent-100 tint. */
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
        !selected -> Color.Transparent
        style == ChipStyle.Filter -> c.accent
        else -> c.accent100
    }
    val textColor = if (selected && style == ChipStyle.Filter) c.bg else c.text
    Box(
        modifier = modifier
            .background(fill)
            .border(1.dp, if (selected) c.accent else c.divider)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = if (style == ChipStyle.Filter) 14.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = VitalsType.control, color = textColor, maxLines = 1)
    }
}

/** Square on/off switch; M3's Switch has a rounded track the design system doesn't allow. */
@Composable
fun SquareSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Vitals.colors
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 4.dp, label = "thumb")
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .size(width = 52.dp, height = 32.dp)
            .background(if (checked) c.accent else Color.Transparent)
            .border(1.dp, if (checked) c.accent else c.text(0.45f))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(24.dp)
                .background(if (checked) c.bg else c.text(0.55f)),
        )
    }
}
