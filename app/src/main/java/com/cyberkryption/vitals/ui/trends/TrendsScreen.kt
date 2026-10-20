package com.cyberkryption.vitals.ui.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.PeriodFormat
import com.cyberkryption.vitals.core.Span
import com.cyberkryption.vitals.core.Status
import com.cyberkryption.vitals.core.Summary
import com.cyberkryption.vitals.core.Tone
import com.cyberkryption.vitals.ui.components.AdaptiveTiles
import com.cyberkryption.vitals.ui.components.BlueprintCard
import com.cyberkryption.vitals.ui.components.IconAction
import com.cyberkryption.vitals.ui.components.Kicker
import com.cyberkryption.vitals.ui.components.OutlinedIconAction
import com.cyberkryption.vitals.ui.components.Scale
import com.cyberkryption.vitals.ui.components.ScreenHeader
import com.cyberkryption.vitals.ui.components.Segmented
import com.cyberkryption.vitals.ui.components.bottomRule
import com.cyberkryption.vitals.ui.components.shortLabel
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

@Composable
fun TrendsScreen(
    modifier: Modifier = Modifier,
    viewModel: TrendsViewModel = viewModel(factory = TrendsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val c = Vitals.colors

    LaunchedEffect(viewModel) {
        viewModel.shareRequests.collect { context.startActivity(it) }
    }

    Column(modifier.fillMaxSize().background(c.bg)) {
        ScreenHeader(title = stringResource(R.string.trends_title)) {
            ExportMenu(onExport = viewModel::export)
        }
        val s = state ?: return@Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Segmented(
                    options = listOf(Span.WEEK to stringResource(R.string.span_week), Span.MONTH to stringResource(R.string.span_month)),
                    selected = s.span,
                    onSelect = viewModel::setSpan,
                )
                Spacer(Modifier.weight(1f))
                OutlinedIconAction(R.drawable.ic_chevron_left, stringResource(R.string.action_previous_period), viewModel::previous)
                Text(
                    PeriodFormat.label(s.period, locale),
                    style = VitalsType.stepper,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 108.dp),
                )
                OutlinedIconAction(
                    R.drawable.ic_chevron_right,
                    stringResource(R.string.action_next_period),
                    viewModel::next,
                    enabled = !s.isCurrent,
                )
            }

            BlueprintCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Kicker(stringResource(R.string.metric_blood_pressure), Modifier.weight(1f).alignByBaseline())
                    val bp = s.summary.bloodPressure
                    Text(
                        bp?.let { "${it.systolic.average}/${it.diastolic.average}" } ?: stringResource(R.string.value_none),
                        style = VitalsType.chartValue,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(stringResource(R.string.unit_avg_mmhg), style = VitalsType.meta, color = c.text(0.6f), modifier = Modifier.alignByBaseline())
                }
                BloodPressureChart(s.days, s.span, locale)
                ChartLegend()
            }

            AdaptiveTiles(
                minWidth = 156.dp,
                gap = 20.dp,
                tiles = listOf(
                    { m ->
                        LineTile(
                            label = stringResource(R.string.metric_heart_rate),
                            summary = s.summary.pulse,
                            unit = stringResource(R.string.unit_bpm),
                            values = s.days.map { it.pulse },
                            scale = Scale.covering(55f, 85f, s.days.mapNotNull { it.pulse }),
                            meta = s.summary.pulse?.let { stringResource(R.string.trends_resting_range, it.min, it.max) }
                                ?: stringResource(R.string.dashboard_no_data),
                            modifier = m,
                        )
                    },
                    { m ->
                        LineTile(
                            label = stringResource(R.string.metric_spo2),
                            summary = s.summary.spo2,
                            unit = stringResource(R.string.unit_percent),
                            values = s.days.map { it.spo2 },
                            scale = Scale.covering(90f, 100f, s.days.mapNotNull { it.spo2 }),
                            threshold = 95f,
                            meta = stringResource(R.string.trends_spo2_threshold),
                            modifier = m,
                        )
                    },
                ),
            )

            Categories(s.categories)
            SummaryTable(s.summary.bloodPressure?.systolic, s.summary.bloodPressure?.diastolic, s.summary.pulse, s.summary.spo2)
        }
    }
}

@Composable
private fun ExportMenu(onExport: (ExportFormat) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val c = Vitals.colors
    Box {
        IconAction(R.drawable.ic_download, stringResource(R.string.action_export), onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = c.bg,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.border(1.dp, c.divider),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_csv), style = VitalsType.bodyMedium) },
                onClick = {
                    open = false
                    onExport(ExportFormat.CSV)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_pdf), style = VitalsType.bodyMedium) },
                onClick = {
                    open = false
                    onExport(ExportFormat.PDF)
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend() {
    val c = Vitals.colors
    val threshold = c.tone(Tone.STAGE1).base
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.trends_bar_legend), style = VitalsType.caption, color = c.text(0.65f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(width = 14.dp, height = 1.dp)
                    .drawBehind {
                        drawLine(
                            threshold,
                            Offset(0f, 0f),
                            Offset(size.width, 0f),
                            1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
                        )
                    },
            )
            Text(stringResource(R.string.trends_threshold_legend), style = VitalsType.caption, color = c.text(0.65f))
        }
    }
}

@Composable
private fun LineTile(
    label: String,
    summary: Summary?,
    unit: String,
    values: List<Int?>,
    scale: Scale,
    meta: String,
    modifier: Modifier,
    threshold: Float? = null,
) {
    val c = Vitals.colors
    BlueprintCard(modifier, spacing = 8.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Kicker(label, Modifier.weight(1f).alignByBaseline())
            Text(summary?.average?.toString() ?: stringResource(R.string.value_none), style = VitalsType.rowValue, modifier = Modifier.alignByBaseline())
            Text(unit, style = VitalsType.caption, color = c.text(0.6f), modifier = Modifier.alignByBaseline())
        }
        DailyLineChart(values, scale, threshold = threshold)
        Text(meta, style = VitalsType.caption, color = c.muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Categories(categories: List<Pair<Status, Int>>) {
    val c = Vitals.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.trends_categories), style = VitalsType.section)
        if (categories.isEmpty()) {
            Text(stringResource(R.string.trends_categories_empty), style = VitalsType.bodyMedium, color = c.muted)
            return@Column
        }
        Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            categories.forEach { (status, n) ->
                Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(c.tone(status.tone).base))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { (status, n) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(c.tone(status.tone).base))
                    Text(stringResource(status.shortLabel()), style = VitalsType.meta)
                    Text(n.toString(), style = VitalsType.metaNumbers, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryTable(systolic: Summary?, diastolic: Summary?, pulse: Summary?, spo2: Summary?) {
    val c = Vitals.colors
    val none = stringResource(R.string.value_none)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.trends_summary), style = VitalsType.section)
        Column {
            Row(Modifier.fillMaxWidth().bottomRule(c.divider).padding(vertical = 8.dp)) {
                listOf(R.string.summary_metric, R.string.summary_avg, R.string.summary_min, R.string.summary_max).forEachIndexed { i, label ->
                    Text(
                        stringResource(label).uppercase(),
                        style = VitalsType.overline,
                        color = c.text(0.6f),
                        textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                        modifier = if (i == 0) Modifier.weight(1.6f) else Modifier.weight(1f),
                    )
                }
            }
            listOf(
                R.string.summary_systolic to systolic,
                R.string.summary_diastolic to diastolic,
                R.string.summary_pulse to pulse,
                R.string.summary_spo2 to spo2,
            ).forEach { (label, s) ->
                Row(Modifier.fillMaxWidth().bottomRule(c.text(0.08f)).padding(vertical = 9.dp)) {
                    Text(stringResource(label), style = VitalsType.bodyMedium, modifier = Modifier.weight(1.6f))
                    TableNumber(s?.average?.toString() ?: none, bold = true)
                    TableNumber(s?.min?.toString() ?: none)
                    TableNumber(s?.max?.toString() ?: none)
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableNumber(text: String, bold: Boolean = false) {
    Text(
        text,
        style = VitalsType.bodyMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f),
    )
}
