package com.cyberkryption.vitals.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.cyberkryption.vitals.ui.components.DialogText
import com.cyberkryption.vitals.ui.components.IconAction
import com.cyberkryption.vitals.ui.components.IconCircle
import com.cyberkryption.vitals.ui.components.PrimaryButton
import com.cyberkryption.vitals.ui.components.ReadingRow
import com.cyberkryption.vitals.ui.components.Scale
import com.cyberkryption.vitals.ui.components.SecondaryButton
import com.cyberkryption.vitals.ui.components.Segmented
import com.cyberkryption.vitals.ui.components.Sparkline
import com.cyberkryption.vitals.ui.components.StatusBadge
import com.cyberkryption.vitals.ui.components.TextAction
import com.cyberkryption.vitals.ui.components.ValueWithUnit
import com.cyberkryption.vitals.ui.components.VitalsCard
import com.cyberkryption.vitals.ui.components.VitalsDialog
import com.cyberkryption.vitals.ui.components.VitalsLogo
import com.cyberkryption.vitals.ui.components.dots
import com.cyberkryption.vitals.ui.components.polyline
import com.cyberkryption.vitals.ui.rememberDateFormats
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType
import java.time.LocalDate

@Composable
fun DashboardScreen(
    onScan: () -> Unit,
    onImport: () -> Unit,
    onOpenReading: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onOpenTrends: () -> Unit,
    onSettings: () -> Unit,
    showActions: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formats = rememberDateFormats()
    val c = Vitals.colors

    Column(modifier.fillMaxSize().background(c.bg)) {
        AppHeader(onSettings)
        val s = state
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (s == null) return@Column
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.nav_today), style = VitalsType.screenTitle, modifier = Modifier.semantics { heading() })
                Text(formats.day.format(s.today), style = VitalsType.subtitle, color = c.muted)
            }
            Segmented(
                options = listOf(Span.WEEK to stringResource(R.string.span_7_days), Span.MONTH to stringResource(R.string.span_30_days)),
                selected = s.span,
                onSelect = viewModel::setSpan,
                height = 40.dp,
                fillWidth = true,
                modifier = Modifier.fillMaxWidth(),
            )
            BloodPressureCard(s.summary, s.span, onOpenTrends)
            AdaptiveTiles(
                minWidth = 150.dp,
                gap = 14.dp,
                tiles = listOf(
                    { m -> HeartRateCard(s.summary.pulse, s.summary.pulseSeries, onOpenTrends, m) },
                    { m -> Spo2Card(s.summary.spo2, s.summary.spo2Series, onOpenTrends, m) },
                ),
            )
            Recent(s.latest, s.today, formats, onOpenReading, onSeeAll)
        }
        if (showActions) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.action_scan),
                    onClick = onScan,
                    icon = R.drawable.ic_camera,
                    height = 50.dp,
                    modifier = Modifier.weight(1.3f),
                )
                SecondaryButton(
                    text = stringResource(R.string.action_import_short),
                    onClick = onImport,
                    icon = R.drawable.ic_upload,
                    height = 50.dp,
                    modifier = Modifier.weight(1f),
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

/** The mark, the app name and Settings. */
@Composable
private fun AppHeader(onSettings: () -> Unit) {
    Row(
        Modifier.padding(start = 20.dp, top = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VitalsLogo(Modifier.size(32.dp))
        Text(stringResource(R.string.app_name), style = VitalsType.appTitle, modifier = Modifier.weight(1f))
        IconAction(R.drawable.ic_settings, stringResource(R.string.settings_title), onSettings)
    }
}

@Composable
private fun CardHeader(icon: Int, container: Color, tint: Color, title: String, iconSize: Dp = 40.dp) {
    val c = Vitals.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconCircle(icon, container, tint, size = iconSize, iconSize = iconSize / 2)
        Text(title, style = VitalsType.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
    }
}

/** The headline card: the period's average, its category, the ranges and a compact trend. */
@Composable
private fun BloodPressureCard(summary: VitalsSummary, span: Span, onOpen: () -> Unit) {
    val c = Vitals.colors
    val bp = summary.bloodPressure
    val tint = lerp(c.surface, c.primaryContainer, if (c.isDark) 0.25f else 0.3f)
    VitalsCard(Modifier.fillMaxWidth(), onClick = onOpen, color = tint, contentPadding = PaddingValues(16.dp), spacing = 8.dp) {
        CardHeader(R.drawable.ic_heart_pulse, c.primaryContainer, c.onPrimaryContainer, stringResource(R.string.dashboard_bp_title))
        if (bp == null) {
            Text(stringResource(R.string.value_none), style = VitalsType.hero)
            Text(
                stringResource(if (span == Span.WEEK) R.string.dashboard_empty_week else R.string.dashboard_empty_month),
                style = VitalsType.bodyMedium,
                color = c.muted,
            )
            return@VitalsCard
        }
        val spoken = stringResource(R.string.a11y_bp_value, bp.systolic.average, bp.diastolic.average)
        ValueWithUnit(
            value = "${bp.systolic.average} / ${bp.diastolic.average}",
            unit = stringResource(R.string.unit_mmhg),
            valueStyle = VitalsType.hero,
            modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusBadge(bp.status)
            Text(pluralStringResource(R.plurals.dashboard_readings, bp.count, bp.count), style = VitalsType.metaNumbers, color = c.muted)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RangeStat(stringResource(R.string.field_systolic), bp.systolic)
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).fillMaxHeight().background(c.divider))
            RangeStat(stringResource(R.string.field_diastolic), bp.diastolic)
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).fillMaxHeight().background(c.divider))
            val scale = Scale.covering(60f, 150f, summary.systolicSeries, summary.diastolicSeries)
            val dotRing = tint
            Canvas(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .padding(vertical = 4.dp),
            ) {
                polyline(summary.diastolicSeries, scale, c.primary.copy(alpha = 0.45f), 2.dp)
                polyline(summary.systolicSeries, scale, c.primary, 2.dp)
                dots(summary.systolicSeries, scale, c.primary, dotRing, radius = 2.5.dp)
            }
        }
    }
}

@Composable
private fun RangeStat(label: String, summary: Summary) {
    val c = Vitals.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = VitalsType.meta, color = c.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.range, summary.min, summary.max),
                style = VitalsType.rowTitle,
                modifier = Modifier.alignByBaseline(),
            )
            Text(stringResource(R.string.unit_mmhg), style = VitalsType.caption, color = c.muted, modifier = Modifier.alignByBaseline())
        }
    }
}

@Composable
private fun HeartRateCard(pulse: Summary?, series: List<Int>, onOpen: () -> Unit, modifier: Modifier) {
    val c = Vitals.colors
    MetricCard(
        icon = R.drawable.ic_heart_filled,
        container = c.primaryContainer,
        tint = c.primary,
        title = stringResource(R.string.metric_heart_rate),
        value = pulse?.average,
        unit = stringResource(R.string.unit_bpm),
        series = series,
        scale = Scale.covering(55f, 85f, series),
        line = c.primary,
        onOpen = onOpen,
        modifier = modifier,
    )
}

@Composable
private fun Spo2Card(spo2: Summary?, series: List<Int>, onOpen: () -> Unit, modifier: Modifier) {
    val c = Vitals.colors
    MetricCard(
        icon = R.drawable.ic_droplet_filled,
        container = c.restContainer,
        tint = c.rest,
        title = stringResource(R.string.metric_spo2),
        value = spo2?.average,
        unit = stringResource(R.string.unit_percent),
        series = series,
        scale = Scale.covering(92f, 100f, series),
        line = c.rest,
        onOpen = onOpen,
        modifier = modifier,
    )
}

@Composable
private fun MetricCard(
    icon: Int,
    container: Color,
    tint: Color,
    title: String,
    value: Int?,
    unit: String,
    series: List<Int>,
    scale: Scale,
    line: Color,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val c = Vitals.colors
    VitalsCard(modifier, onClick = onOpen, contentPadding = PaddingValues(14.dp), spacing = 8.dp) {
        CardHeader(icon, container, tint, title, iconSize = 34.dp)
        if (value == null) {
            Text(stringResource(R.string.value_none), style = VitalsType.tileValue)
        } else {
            ValueWithUnit(value.toString(), unit, VitalsType.tileValue)
        }
        if (series.isEmpty()) {
            Text(stringResource(R.string.dashboard_no_data), style = VitalsType.meta, color = c.muted, modifier = Modifier.height(34.dp))
        } else {
            Sparkline(series, scale, line, Modifier.fillMaxWidth().height(34.dp), area = true)
        }
    }
}

@Composable
private fun Recent(
    latest: List<Reading>,
    today: LocalDate,
    formats: DateFormats,
    onOpenReading: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    val c = Vitals.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.dashboard_recent),
                style = VitalsType.section,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (latest.isNotEmpty()) TextAction(stringResource(R.string.action_see_all), onSeeAll, style = VitalsType.link)
        }
        if (latest.isEmpty()) {
            VitalsCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dashboard_latest_empty), style = VitalsType.bodyMedium, color = c.muted)
            }
        }
        latest.forEach { reading ->
            ReadingRow(
                reading = reading,
                title = whenTaken(reading, formats, today),
                onClick = { onOpenReading(reading.id) },
            )
        }
    }
}

/** "8:42 AM" today, "Yesterday, 7:30 PM", then "Mon, Oct 19, 7:30 PM". */
@Composable
private fun whenTaken(reading: Reading, formats: DateFormats, today: LocalDate): String {
    val day = formats.date(reading.takenAt)
    val time = formats.format(formats.time, reading.takenAt)
    return when (day) {
        today -> time
        today.minusDays(1) -> stringResource(R.string.yesterday_at, time)
        else -> stringResource(R.string.day_at, formats.day.format(day), time)
    }
}
