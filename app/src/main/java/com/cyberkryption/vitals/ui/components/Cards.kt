package com.cyberkryption.vitals.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsColors
import com.cyberkryption.vitals.ui.theme.VitalsShape

/**
 * Rounded surface: white with a soft shadow and a faint outline in light mode, a lifted
 * forest-green surface with an outline in dark mode.
 */
fun Modifier.cardSurface(
    colors: VitalsColors,
    shape: Shape = VitalsShape.card,
    color: Color = colors.surface,
    border: Color = colors.border,
    elevation: Dp = 4.dp,
): Modifier = this
    .then(
        if (colors.isDark) {
            Modifier
        } else {
            Modifier.shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = colors.text.copy(alpha = 0.08f),
                spotColor = colors.text.copy(alpha = 0.10f),
            )
        },
    )
    .clip(shape)
    .background(color)
    .border(1.dp, border, shape)

/** A metric card. Clickable cards announce themselves as buttons. */
@Composable
fun VitalsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = VitalsShape.card,
    color: Color = Vitals.colors.surface,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Vitals.colors
    Column(
        modifier = modifier
            .cardSurface(c, shape, color)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Rounded photo area; an image glyph shows through until the photo loads. */
@Composable
fun PhotoFrame(modifier: Modifier = Modifier, shape: Shape = VitalsShape.photo, content: @Composable BoxScope.() -> Unit) {
    val c = Vitals.colors
    Box(
        modifier = modifier
            .clip(shape)
            .background(c.surfaceVariant)
            .border(1.dp, c.border, shape),
    ) {
        Icon(
            painterResource(R.drawable.ic_image),
            contentDescription = null,
            tint = c.muted.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.Center).size(32.dp),
        )
        content()
    }
}

/** An icon on a tonal circle, as on the dashboard cards and reading rows. */
@Composable
fun IconCircle(
    @DrawableRes icon: Int,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) {
    Box(modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** A 1dp line along the bottom edge, for table rows and chart baselines. */
fun Modifier.bottomRule(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawLine(color, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
}

fun Modifier.topRule(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
}
