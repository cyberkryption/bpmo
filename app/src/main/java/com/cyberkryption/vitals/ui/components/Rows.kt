package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.status
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsShape
import com.cyberkryption.vitals.ui.theme.VitalsType
import com.cyberkryption.vitals.ui.unitText
import com.cyberkryption.vitals.ui.valueText

/** A reading as a rounded list item: type icon, when it was taken, the values, category and a chevron. */
@Composable
fun ReadingRow(
    reading: Reading,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = reading.note?.takeIf { it.isNotBlank() },
    selected: Boolean = false,
) {
    val c = Vitals.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .cardSurface(
                colors = c,
                shape = VitalsShape.item,
                color = if (selected) c.primaryContainer else c.surface,
                border = if (selected) c.primary else c.border,
                elevation = 2.dp,
            )
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeIcon(reading.type, onTint = selected)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // The category sits beside the time so the values below get the full width.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = VitalsType.rowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                StatusBadge(reading.status(Vitals.guideline), small = true)
            }
            Text(
                "${reading.valueText()} ${reading.unitText()}",
                style = VitalsType.rowSubtitle,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note != null) {
                Text(note, style = VitalsType.meta, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
    }
}

/**
 * Blood pressure on the teal circle, SpO₂ on the lavender one. [onTint] puts the circle on the
 * surface colour instead, for rows that are already tinted.
 */
@Composable
fun TypeIcon(type: ReadingType, modifier: Modifier = Modifier, onTint: Boolean = false) {
    val c = Vitals.colors
    when (type) {
        ReadingType.BP -> IconCircle(R.drawable.ic_heart_pulse, if (onTint) c.surface else c.primaryContainer, c.onPrimaryContainer, modifier)
        ReadingType.OXIMETER -> IconCircle(R.drawable.ic_droplet, if (onTint) c.surface else c.restContainer, c.restInk, modifier)
    }
}
