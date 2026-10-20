package com.cyberkryption.vitals.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.ui.components.registrationMarks
import com.cyberkryption.vitals.ui.components.topRule
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

enum class TopLevel(@StringRes val label: Int, @DrawableRes val icon: Int) {
    TODAY(R.string.nav_today, R.drawable.ic_house),
    HISTORY(R.string.nav_history, R.drawable.ic_list),
    TRENDS(R.string.nav_trends, R.drawable.ic_chart_line),
}

/** Compact: 80dp bar with a square 64×32 selection tint. */
@Composable
fun VitalsNavigationBar(selected: TopLevel?, onSelect: (TopLevel) -> Unit, modifier: Modifier = Modifier) {
    val c = Vitals.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.bg)
            .topRule(c.divider)
            .navigationBarsPadding()
            .height(80.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevel.entries.forEach { item ->
            NavItem(item, item == selected, { onSelect(item) }, indicatorWidth = 64.dp, modifier = Modifier.weight(1f))
        }
    }
}

/** Medium and expanded: 88dp rail with the Scan action at the top. */
@Composable
fun VitalsNavigationRail(
    selected: TopLevel?,
    onSelect: (TopLevel) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Vitals.colors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .background(c.bg)
            .drawBehind {
                val x = size.width - 0.5.dp.toPx()
                drawLine(c.divider, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
            .statusBarsPadding()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Box(
            modifier = Modifier
                .registrationMarks(c.mark)
                .size(56.dp)
                .background(c.accent)
                .border(1.dp, c.accent)
                .clickable(role = Role.Button, onClick = onScan),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_camera),
                contentDescription = stringResource(R.string.action_scan),
                tint = c.bg,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TopLevel.entries.forEach { item ->
                NavItem(item, item == selected, { onSelect(item) }, indicatorWidth = 56.dp)
            }
        }
    }
}

@Composable
private fun NavItem(
    item: TopLevel,
    selected: Boolean,
    onClick: () -> Unit,
    indicatorWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val c = Vitals.colors
    Column(
        modifier = modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(width = indicatorWidth, height = 32.dp)
                .background(if (selected) c.accent100 else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(item.icon),
                contentDescription = null,
                tint = if (selected) c.accent800 else c.text,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            stringResource(item.label),
            style = VitalsType.navLabel,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = c.text,
        )
    }
}
