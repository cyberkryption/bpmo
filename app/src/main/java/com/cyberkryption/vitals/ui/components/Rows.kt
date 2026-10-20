package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.status
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType
import com.cyberkryption.vitals.ui.unitText
import com.cyberkryption.vitals.ui.valueText

/** A reading in a list: leading thumbnail or icon, value, unit and pulse, meta line, category tag. */
@Composable
fun ReadingRow(
    reading: Reading,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(vertical = 12.dp),
    leading: @Composable () -> Unit,
) {
    val c = Vitals.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) c.accent100 else Color.Transparent)
            .drawBehind {
                if (selected) drawRect(c.accent, size = Size(3.dp.toPx(), size.height))
            }
            .bottomRule(c.hairline)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(reading.valueText(), style = VitalsType.rowValue, modifier = Modifier.alignByBaseline())
                Text(
                    reading.unitText(),
                    style = VitalsType.meta,
                    color = c.text(0.6f),
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                )
            }
            Text(meta, style = VitalsType.meta, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        StatusTag(reading.status)
    }
}

/** Photo thumbnail with a hairline frame; hatching shows through until the image loads. */
@Composable
fun PhotoThumb(uri: String?, size: Dp, modifier: Modifier = Modifier) {
    val c = Vitals.colors
    Box(
        modifier
            .size(size)
            .border(1.dp, c.divider)
            .hatching(c.text(0.07f), period = 8.dp),
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().padding(1.dp),
            )
        }
    }
}

/** 40dp outlined type icon used in History. */
@Composable
fun TypeIcon(type: ReadingType, modifier: Modifier = Modifier) {
    val c = Vitals.colors
    Box(modifier.size(40.dp).border(1.dp, c.divider), contentAlignment = Alignment.Center) {
        Icon(
            painterResource(if (type == ReadingType.BP) R.drawable.ic_activity else R.drawable.ic_droplet),
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(20.dp),
        )
    }
}
