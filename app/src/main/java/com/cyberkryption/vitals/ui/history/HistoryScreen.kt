package com.cyberkryption.vitals.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.ui.DateFormats
import com.cyberkryption.vitals.ui.components.ChipStyle
import com.cyberkryption.vitals.ui.components.ChoiceChip
import com.cyberkryption.vitals.ui.components.IconAction
import com.cyberkryption.vitals.ui.components.Kicker
import com.cyberkryption.vitals.ui.components.ReadingRow
import com.cyberkryption.vitals.ui.components.ScreenHeader
import com.cyberkryption.vitals.ui.components.TypeIcon
import com.cyberkryption.vitals.ui.detail.ReadingDetailScreen
import com.cyberkryption.vitals.ui.isMediumOrWider
import com.cyberkryption.vitals.ui.rememberDateFormats
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * History list with Reading detail. One pane at a time when compact; at medium width and up
 * the 360dp list sits beside the detail, which then hides its back arrow.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HistoryScreen(
    onEdit: (Reading) -> Unit,
    onDetailOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val twoPane = adaptiveInfo.windowSizeClass.isMediumOrWider
    val directive = calculatePaneScaffoldDirective(adaptiveInfo).copy(
        maxHorizontalPartitions = if (twoPane) 2 else 1,
        horizontalPartitionSpacerSize = 0.dp,
    )
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>(scaffoldDirective = directive)
    val scope = rememberCoroutineScope()
    val selectedId = navigator.currentDestination?.contentKey
    val listShown = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
    val detailShown = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

    LaunchedEffect(listShown, detailShown) { onDetailOnlyChange(detailShown && !listShown) }
    BackHandler(navigator.canNavigateBack()) { scope.launch { navigator.navigateBack() } }

    val c = Vitals.colors
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier.background(c.bg),
        listPane = {
            AnimatedPane {
                HistoryList(
                    viewModel = viewModel,
                    selectedId = if (detailShown) selectedId else null,
                    onSelect = { id -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) } },
                    modifier = if (detailShown) {
                        Modifier.drawBehind {
                            val x = size.width - 0.5.dp.toPx()
                            drawLine(c.divider, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                        }
                    } else {
                        Modifier
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val id = selectedId
                if (id == null) {
                    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.history_select), style = VitalsType.bodyMedium, color = c.muted)
                    }
                } else {
                    ReadingDetailScreen(
                        readingId = id,
                        showBack = !listShown,
                        onBack = { scope.launch { navigator.navigateBack() } },
                        onEdit = onEdit,
                        onDeleted = { scope.launch { navigator.navigateBack() } },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(
    viewModel: HistoryViewModel,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formats = rememberDateFormats()
    var searching by rememberSaveable { mutableStateOf(false) }
    val c = Vitals.colors

    Column(modifier.fillMaxSize().background(c.bg)) {
        if (searching) {
            SearchBar(
                query = state?.query.orEmpty(),
                onQueryChange = viewModel::setQuery,
                onClose = {
                    searching = false
                    viewModel.setQuery("")
                },
            )
        } else {
            ScreenHeader(title = stringResource(R.string.history_title)) {
                IconAction(R.drawable.ic_search, stringResource(R.string.action_search), onClick = { searching = true }, iconSize = 22.dp)
            }
        }
        val s = state ?: return@Column
        Row(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryFilter.entries.forEach { f ->
                ChoiceChip(
                    label = stringResource(
                        when (f) {
                            HistoryFilter.ALL -> R.string.filter_all
                            HistoryFilter.BP -> R.string.metric_blood_pressure
                            HistoryFilter.SPO2 -> R.string.metric_spo2
                        },
                    ),
                    selected = s.filter == f,
                    onClick = { viewModel.setFilter(f) },
                    style = ChipStyle.Filter,
                )
            }
        }
        if (s.groups.isEmpty()) {
            Text(
                stringResource(if (s.hasReadings) R.string.history_no_matches else R.string.history_empty),
                style = VitalsType.bodyMedium,
                color = c.muted,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Rows bleed 8dp into the 20dp gutter so the selection tint has room; content stays on the 20dp line.
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
        ) {
            s.groups.forEach { group ->
                stickyHeader(key = "day-${group.date}") {
                    Kicker(
                        dayTitle(group, s.today, formats),
                        Modifier
                            .fillMaxWidth()
                            .background(c.bg)
                            .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 4.dp),
                    )
                }
                items(group.readings, key = { it.id }) { reading ->
                    ReadingRow(
                        reading = reading,
                        meta = listOfNotNull(
                            formats.format(formats.time, reading.takenAt),
                            reading.note?.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        onClick = { onSelect(reading.id) },
                        selected = reading.id == selectedId,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        TypeIcon(reading.type)
                    }
                }
            }
        }
    }
}

@Composable
private fun dayTitle(group: DayGroup, today: LocalDate, formats: DateFormats): String {
    val day = formats.day.format(group.date)
    return if (group.date == today) stringResource(R.string.history_today, day) else day
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val c = Vitals.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = VitalsType.bodyLarge.copy(color = c.text),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .background(c.surface)
                        .border(1.dp, c.divider)
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.history_search_hint), style = VitalsType.bodyLarge, color = c.text(0.5f))
                    }
                    inner()
                }
            },
        )
        IconAction(R.drawable.ic_x, stringResource(R.string.action_close_search), onClick = onClose)
    }
}
