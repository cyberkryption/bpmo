package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.ui.theme.VitalsType

/** Top-level screen header: optional kicker above a 30sp title, actions on the right. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (kicker != null) Kicker(kicker)
            Text(title, style = VitalsType.screenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

/** Sub-screen app bar: back or close, a 22sp title, trailing actions. */
@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: Int = R.drawable.ic_arrow_left,
    titleStartPadding: Dp = 4.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(start = 4.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconAction(
                icon = navigationIcon,
                contentDescription = stringResource(
                    if (navigationIcon == R.drawable.ic_x) R.string.action_close else R.string.action_back,
                ),
                onClick = onBack,
            )
        }
        Text(
            text = title,
            style = VitalsType.appBarTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) titleStartPadding else 16.dp),
        )
        actions()
    }
}
