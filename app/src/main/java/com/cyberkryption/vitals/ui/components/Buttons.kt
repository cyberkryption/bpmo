package com.cyberkryption.vitals.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsShape
import com.cyberkryption.vitals.ui.theme.VitalsType

private const val DISABLED_ALPHA = 0.4f

@Composable
private fun RowScope.ButtonContent(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
    }
    Text(text, style = VitalsType.button, maxLines = 1)
}

/** The main action on a screen: a filled pill. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    height: Dp = 52.dp,
) {
    val c = Vitals.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = VitalsShape.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.primary,
            contentColor = c.onPrimary,
            disabledContainerColor = c.primary.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = c.onPrimary.copy(alpha = 0.8f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        ButtonContent(text, icon)
    }
}

/** A secondary action beside the primary one: an outlined pill on the surface colour. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    height: Dp = 52.dp,
) {
    val c = Vitals.colors
    val content = if (c.isDark) c.text else c.primaryInk
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = VitalsShape.pill,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (c.isDark) Color.Transparent else c.surface,
            contentColor = content,
            disabledContentColor = content.copy(alpha = DISABLED_ALPHA),
        ),
        border = BorderStroke(1.dp, if (c.isDark) c.muted.copy(alpha = 0.5f) else c.border),
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        ButtonContent(text, icon)
    }
}

/** Text-only action (Change, See all, dialog buttons). */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Vitals.colors.primaryInk,
    style: TextStyle = VitalsType.action,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = VitalsShape.pill,
        colors = ButtonDefaults.textButtonColors(contentColor = color, disabledContentColor = color.copy(alpha = DISABLED_ALPHA)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, style = style, maxLines = 1)
    }
}

/** 48dp round touch target around an icon. */
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
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint, disabledContentColor = tint.copy(alpha = DISABLED_ALPHA)),
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

/** Round outlined icon button, as in the period stepper. */
@Composable
fun OutlinedIconAction(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 18.dp,
) {
    val c = Vitals.colors
    Box(
        modifier = modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = false, radius = size / 2),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = contentDescription, tint = c.text, modifier = Modifier.size(iconSize))
        }
    }
}
