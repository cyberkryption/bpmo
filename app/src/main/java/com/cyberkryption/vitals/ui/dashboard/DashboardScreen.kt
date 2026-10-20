package com.cyberkryption.vitals.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.Span
import com.cyberkryption.vitals.core.Summary
import com.cyberkryption.vitals.core.VitalsSummary
import com.cyberkryption.vitals.ui.DateFormats
import com.cyberkryption.vitals.ui.components.AdaptiveTiles
import com.cyberkryption.vitals.ui.components.BlueprintCard
import com.cyberkryption.vitals.ui.components.DialogText
import com.cyberkryption.vitals.ui.components.IconAction
import com.cyberkryption.vitals.ui.components.Kicker
import com.cyberkryption.vitals.ui.components.OutlinedIconAction
import com.cyberkryption.vitals.ui.components.PhotoThumb
import com.cyberkryption.vitals.ui.components.PrimaryButton
import com.cyberkryption.vitals.ui.components.ReadingRow
import com.cyberkryption.vitals.ui.components.Scale
import com.cyberkryption.vitals.ui.components.ScreenHeader
import com.cyberkryption.vitals.ui.components.Segmented
import com.cyberkryption.vitals.ui.components.Sparkline
import com.cyberkryption.vitals.ui.components.StatusTag
import com.cyberkryption.vitals.ui.components.VitalsDialog
import com.cyberkryption.vitals.ui.components.bottomRule
import com.cyberkryption.vitals.ui.components.dashedLine
import com.cyberkryption.vitals.ui.components.polyline
import com.cyberkryption.vitals.ui.rememberDateFormats
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType
import java.time.Instant
import java.time.LocalDate

@Composable
fun DashboardScreen(
    onScan: () -> Unit,
    onImport: () -> Unit,
    onOpenReading: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onSettings: () -> Unit,
    showFabs: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formats = rememberDateFormats()
    val c = Vitals.colors

    Box(modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(R.string.app_name),
                kicker = state?.let { formats.day.format(it.today) },
            ) {
                OverflowMenu(onImport = onImport, onSettings = onSettings)
            }
            val s = state ?: return@Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = if (showFabs) 140.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Segmented(
                    options = listOf(Span.WEEK to stringResource(R.string.span_7_days), Span.MONTH to stringResource(R.string.span_30_days)),
                    selected = s.span,
                    onSelect = viewModel::setSpan,
                )
                Tiles(s)
                Latest(s.latest, formats, onOpenReading, onSeeAll)
            }
        }

        if (showFabs) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedIconAction(
                    icon = R.drawable.ic_image,
                    contentDescription = stringResource(R.string.action_import),
                    onClick = onImport,
                    size = 56.dp,
                    iconSize = 24.dp,
                    fill = c.bg,
                    elevated = true,
                )
                PrimaryButton(
                    text = stringResource(R.string.action_scan),
                    onClick = onScan,
                    icon = R.drawable.ic_camera,
                    height = 56.dp,
                    elevated = true,
                )
            }
        }
    }

    if (state?.offerLock == true) {
        VitalsDialog(
            title = stringResource(R.string.lock_offer_title),
            onDismissRequest = { viewModel.answerLockOffer(false) },
            confirmText = stringResource(R.string.lock_offer_confirm),
            onConfirm = { viewModel.answerLockOffer(true) },
            dismissText = stringResource(R.string.lock_offer_dismiss),
        ) {
            DialogText(stringResource(R.string.lock_offer_body))
        }
    }
}

@Composable
private fun OverflowMenu(onImport: () -> Unit, onSettings: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val c = Vitals.colors
    Box {
        IconAction(R.drawable.ic_more_vertical, stringResource(R.string.action_more), onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = c.bg,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.border(1.dp, c.divider),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_import), style = VitalsType.bodyMedium) },
                onClick = {
                    open = false
                    onImport()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_title), style = VitalsType.bodyMedium) },
                onClick = {
                    open = false
                    onSettings()
                },
            )
        }
    }
}

@Composable
private fun Tiles(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BloodPressureTile(state.summary, state.span)
        AdaptiveTiles(
            minWidth = 156.dp,
            gap = 20.dp,
            tiles = listOf(
                { m -> HeartRateTile(state.summary.pulse, state.summary.pulseSeries, m) },
                { m -> Spo2Tile(state.summary.spo2, state.summary.spo2Series, m) },
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloodPressureTile(summary: VitalsSummary, span: Span) {
    val c = Vitals.colors
    val bp = summary.bloodPressure
    BlueprintCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_activity), null, tint = c.accent, modifier = Modifier.size(18.dp))
            Kicker(
                stringResource(R.string.dashboard_bp_kicker, stringResource(if (span == Span.WEEK) R.string.span_7_day else R.string.span_30_day)),
                Modifier.weight(1f),
            )
            if (bp != null) StatusTag(bp.status, large = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = bp?.let { "${it.systolic.average}/${it.diastolic.average}" } ?: stringResource(R.string.value_none),
                style = VitalsType.hero,
                modifier = Modifier.alignByBaseline(),
            )
            Text(stringResource(R.string.unit_mmhg), style = VitalsType.bodyMedium, color = c.text(0.6f), modifier = Modifier.alignByBaseline())
        }
        val scale = Scale.covering(60f, 150f, summary.systolicSeries, summary.diastolicSeries)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .bottomRule(c.divider),
        ) {
            val guide = c.text(0.22f)
            dashedLine(guide, scale.y(120f, size.height))
            dashedLine(guide, scale.y(80f, size.height))
            polyline(summary.systolicSeries, scale, c.accent, 2.dp)
            polyline(summary.diastolicSeries, scale, c.accent, 1.5.dp, dashed = true)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (bp == null) {
                Text(stringResource(if (span == Span.WEEK) R.string.dashboard_empty_week else R.string.dashboard_empty_month), style = VitalsType.meta, color = c.muted)
            } else {
                Text(pluralStringResource(R.plurals.dashboard_readings, bp.count, bp.count), style = VitalsType.metaNumbers, color = c.muted)
                Text(stringResource(R.string.dashboard_sys_range, bp.systolic.min, bp.systolic.max), style = VitalsType.metaNumbers, color = c.muted)
                Text(stringResource(R.string.dashboard_dia_range, bp.diastolic.min, bp.diastolic.max), style = VitalsType.metaNumbers, color = c.muted)
            }
        }
    }
}

@Composable
private fun HeartRateTile(pulse: Summary?, series: List<Int>, modifier: Modifier) {
    SmallTile(
        icon = R.drawable.ic_heart,
        label = stringResource(R.string.metric_heart_rate),
        value = pulse?.average,
        unit = stringResource(R.string.unit_bpm),
        series = series,
        scale = Scale.covering(55f, 85f, series),
        meta = pulse?.let { stringResource(R.string.dashboard_hr_range, it.min, it.max) },
        modifier = modifier,
    )
}

@Composable
private fun Spo2Tile(spo2: Summary?, series: List<Int>, modifier: Modifier) {
    SmallTile(
        icon = R.drawable.ic_droplet,
        label = stringResource(R.string.metric_spo2),
        value = spo2?.average,
        unit = stringResource(R.string.unit_percent),
        series = series,
        scale = Scale.covering(92f, 100f, series),
        meta = spo2?.let { stringResource(R.string.dashboard_spo2_lowest, it.min) },
        modifier = modifier,
    )
}

@Composable
private fun SmallTile(
    icon: Int,
    label: String,
    value: Int?,
    unit: String,
    series: List<Int>,
    scale: Scale,
    meta: String?,
    modifier: Modifier,
) {
    val c = Vitals.colors
    BlueprintCard(modifier, spacing = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(icon), null, tint = c.accent700, modifier = Modifier.size(16.dp))
            Kicker(label)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value?.toString() ?: stringResource(R.string.value_none), style = VitalsType.tileValue, modifier = Modifier.alignByBaseline())
            Text(unit, style = VitalsType.control, color = c.text(0.6f), modifier = Modifier.alignByBaseline())
        }
        Sparkline(series, scale, c.accent, Modifier.fillMaxWidth().height(32.dp))
        Text(meta ?: stringResource(R.string.dashboard_no_data), style = VitalsType.metaNumbers, color = c.muted)
    }
}

@Composable
private fun Latest(
    latest: List<Reading>,
    formats: DateFormats,
    onOpenReading: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    val c = Vitals.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dashboard_latest), style = VitalsType.section, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clickable(role = Role.Button, onClick = onSeeAll)
                    .padding(start = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(stringResource(R.string.action_see_all), style = VitalsType.link, color = c.accent700)
            }
        }
        if (latest.isEmpty()) {
            Text(stringResource(R.string.dashboard_latest_empty), style = VitalsType.bodyMedium, color = c.muted, modifier = Modifier.padding(vertical = 12.dp))
        }
        val today = formats.date(Instant.now())
        latest.forEach { reading ->
            ReadingRow(
                reading = reading,
                meta = latestMeta(reading, formats, today),
                onClick = { onOpenReading(reading.id) },
            ) {
                PhotoThumb(reading.photoUri, 48.dp)
            }
        }
    }
}

@Composable
private fun latestMeta(reading: Reading, formats: DateFormats, today: LocalDate): String {
    val day = formats.date(reading.takenAt)
    val time = formats.format(formats.time, reading.takenAt)
    val whenText = if (day == today) stringResource(R.string.today_at, time) else "${formats.day.format(day)} $time"
    return listOfNotNull(whenText, reading.note?.takeIf { it.isNotBlank() }).joinToString(" · ")
}
