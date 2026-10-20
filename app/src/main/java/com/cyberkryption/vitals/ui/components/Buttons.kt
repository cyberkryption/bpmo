package com.cyberkryption.vitals.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

private const val DISABLED_ALPHA = 0.45f

/** FAB shadow (shadow-md: 0 3 10, ink at 16%). Only FABs cast shadows. */
fun Modifier.fabShadow(): Modifier = shadow(
    elevation = 6.dp,
    shape = RectangleShape,
    // Unclipped, so registration marks outside the bounds still draw.
    clip = false,
    ambientColor = Color(0xFF2B2B2D).copy(alpha = 0.16f),
    spotColor = Color(0xFF2B2B2D).copy(alpha = 0.16f),
)

/** The one solid object on a screen: accent fill, background-coloured text and corner marks. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    height: Dp = 52.dp,
    elevated: Boolean = false,
) {
    val colors = Vitals.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(if (elevated) Modifier.fabShadow() else Modifier)
            .registrationMarks(colors.mark)
            .background(if (pressed) colors.accent600 else colors.accent)
            .border(1.dp, colors.accent)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .heightIn(min = height)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.bg, modifier = Modifier.size(22.dp))
        }
        Text(text, style = VitalsType.primaryButton, color = colors.bg, maxLines = 1)
    }
}

/** Hairline-bordered secondary action (Skip, Use device PIN). */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    style: TextStyle = VitalsType.button,
) {
    val colors = Vitals.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(if (pressed) colors.text(0.14f) else Color.Transparent)
            .border(1.dp, colors.divider)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .heightIn(min = height)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = style, color = colors.text, maxLines = 1)
    }
}

/** Text-only action in the heading face (Change, See all, dialog buttons). */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Vitals.colors.accent700,
    style: TextStyle = VitalsType.action,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = style, color = color, maxLines = 1)
    }
}

/** 48dp square touch target around a Lucide icon. */
@Composable
fun IconAction(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 24.dp,
    tint: Color = Vitals.colors.text,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Square outlined icon button (period stepper, Import FAB). */
@Composable
fun OutlinedIconAction(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 18.dp,
    fill: Color = Color.Transparent,
    elevated: Boolean = false,
) {
    val colors = Vitals.colors
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (elevated) Modifier.fabShadow() else Modifier)
            .size(size)
            .background(fill)
            .border(1.dp, colors.divider)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = colors.text, modifier = Modifier.size(iconSize))
    }
}
