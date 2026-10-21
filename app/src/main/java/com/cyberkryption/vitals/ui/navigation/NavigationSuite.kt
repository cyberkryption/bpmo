package com.cyberkryption.vitals.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.ui.components.IconAction
import com.cyberkryption.vitals.ui.components.topRule
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsShape
import com.cyberkryption.vitals.ui.theme.VitalsType

enum class TopLevel(@StringRes val label: Int, @DrawableRes val icon: Int) {
    TODAY(R.string.nav_today, R.drawable.ic_house),
    HISTORY(R.string.nav_history, R.drawable.ic_clock),
    TRENDS(R.string.nav_trends, R.drawable.ic_chart),
    SETTINGS(R.string.nav_settings, R.drawable.ic_settings),
}

@Composable
private fun ItemLabel(item: TopLevel, selected: Boolean) {
    Text(
        stringResource(item.label),
        style = VitalsType.navLabel,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
    )
}

/** Compact: bottom bar with a pill behind the selected icon. */
@Composable
fun VitalsNavigationBar(selected: TopLevel?, onSelect: (TopLevel) -> Unit, modifier: Modifier = Modifier) {
    val c = Vitals.colors
    NavigationBar(
        modifier = modifier.topRule(c.divider),
        containerColor = c.surface,
        tonalElevation = 0.dp,
    ) {
        TopLevel.entries.forEach { item ->
            val isSelected = item == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(item) },
                icon = { Icon(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { ItemLabel(item, isSelected) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.onPrimaryContainer,
                    selectedTextColor = c.primaryInk,
                    indicatorColor = c.primaryContainer,
                    unselectedIconColor = c.muted,
                    unselectedTextColor = c.muted,
                ),
            )
        }
    }
}

/** Medium and expanded: a rail with Scan and Import at the top. */
@Composable
fun VitalsNavigationRail(
    selected: TopLevel?,
    onSelect: (TopLevel) -> Unit,
    onScan: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Vitals.colors
    NavigationRail(
        modifier = modifier.drawBehind {
            val x = size.width - 0.5.dp.toPx()
            drawLine(c.divider, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        },
        containerColor = c.surface,
        header = {
            Column(
                Modifier.padding(top = 12.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FloatingActionButton(
                    onClick = onScan,
                    shape = VitalsShape.item,
                    containerColor = c.primary,
                    contentColor = c.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = stringResource(R.string.action_scan), modifier = Modifier.size(24.dp))
                }
                IconAction(R.drawable.ic_upload, stringResource(R.string.action_import), onImport, tint = c.primaryInk, iconSize = 22.dp)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TopLevel.entries.forEach { item ->
                val isSelected = item == selected
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onSelect(item) },
                    icon = { Icon(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = { ItemLabel(item, isSelected) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = c.onPrimaryContainer,
                        selectedTextColor = c.primaryInk,
                        indicatorColor = c.primaryContainer,
                        unselectedIconColor = c.muted,
                        unselectedTextColor = c.muted,
                    ),
                )
            }
        }
    }
}
