package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Equal-width, equal-height tiles at least [minWidth] wide, like CSS grid
 * `repeat(auto-fit, minmax(minWidth, 1fr))`: with fewer tiles than columns, tiles stretch.
 */
@Composable
fun AdaptiveTiles(
    minWidth: Dp,
    gap: Dp,
    modifier: Modifier = Modifier,
    tiles: List<@Composable (Modifier) -> Unit>,
) {
    BoxWithConstraints(modifier) {
        val columns = ((maxWidth + gap) / (minWidth + gap)).toInt().coerceAtLeast(1)
        val perRow = minOf(columns, tiles.size).coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            tiles.chunked(perRow).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
