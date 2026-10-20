package com.cyberkryption.vitals.ui.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.ui.DateFormats
import com.cyberkryption.vitals.ui.components.AppBar
import com.cyberkryption.vitals.ui.components.Kicker
import com.cyberkryption.vitals.ui.components.PrimaryButton
import com.cyberkryption.vitals.ui.components.hatching
import com.cyberkryption.vitals.ui.components.topRule
import com.cyberkryption.vitals.ui.rememberDateFormats
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

@Composable
fun ImportScreen(
    onClose: () -> Unit,
    onReview: (sessionId: String) -> Unit,
    viewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formats = rememberDateFormats()
    val c = Vitals.colors

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isEmpty() && viewModel.state.value.photos.isEmpty()) onClose() else viewModel.add(uris)
    }
    val openPicker = {
        viewModel.beforePicker()
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    var launched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            openPicker()
        }
    }
    LaunchedEffect(state.sessionId) { state.sessionId?.let(onReview) }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        AppBar(
            title = stringResource(R.string.import_title),
            onBack = onClose,
            navigationIcon = R.drawable.ic_x,
        ) {
            Text(
                pluralStringResource(R.plurals.import_selected, state.selected.size, state.selected.size),
                style = VitalsType.control.copy(fontFeatureSettings = "tnum"),
                color = c.text(0.65f),
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Text(
            stringResource(R.string.import_subtitle),
            style = VitalsType.control,
            color = c.text(0.65f),
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            viewModel.groups(state).forEach { group ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "header-${group.thisWeek}") {
                    Kicker(
                        stringResource(if (group.thisWeek) R.string.import_this_week else R.string.import_earlier),
                        Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(group.photos, key = { it.uri.toString() }) { photo ->
                    PhotoTile(
                        photo = photo,
                        thisWeek = group.thisWeek,
                        order = state.selected.indexOf(photo.uri).takeIf { it >= 0 }?.plus(1),
                        formats = formats,
                        onClick = { viewModel.toggle(photo.uri) },
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .topRule(c.divider)
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val note = when {
                state.failed -> stringResource(R.string.import_failed)
                state.undatedSelected > 0 -> pluralStringResource(R.plurals.import_no_date_note, state.undatedSelected, state.undatedSelected)
                else -> stringResource(R.string.import_note)
            }
            Text(
                note,
                style = VitalsType.meta,
                color = if (state.failed) c.error.ink else c.text(0.65f),
                modifier = Modifier.weight(1f),
            )
            val count = state.selected.size
            PrimaryButton(
                text = if (count > 0) stringResource(R.string.import_review, count) else stringResource(R.string.import_select),
                onClick = { if (count > 0) viewModel.review() else openPicker() },
                enabled = !state.preparing && !state.loading,
            )
        }
    }
}

@Composable
private fun PhotoTile(
    photo: ImportPhoto,
    thisWeek: Boolean,
    order: Int?,
    formats: DateFormats,
    onClick: () -> Unit,
) {
    val c = Vitals.colors
    val selected = order != null
    Box(
        Modifier
            .aspectRatio(1f)
            .hatching(c.text(0.08f), period = 10.dp)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.divider)
            .semantics { this.selected = selected }
            .clickable(role = Role.Checkbox, onClick = onClick),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().padding(if (selected) 2.dp else 1.dp),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .background(if (selected) c.accent else c.bg.copy(alpha = 0.6f))
                .border(1.5.dp, if (selected) c.accent else c.text(0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            if (order != null) {
                Text(order.toString(), style = VitalsType.meta, fontWeight = FontWeight.Bold, color = c.bg)
            }
        }
        val date = photo.date
        Text(
            text = when {
                date == null -> stringResource(R.string.import_no_date)
                thisWeek -> formats.format(formats.weekdayTime, date.instant)
                else -> formats.format(formats.dayMonthTime, date.instant)
            },
            style = VitalsType.caption.copy(fontFeatureSettings = "tnum"),
            color = if (date == null) c.warning.ink else c.text,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(if (selected) 2.dp else 1.dp)
                .background(c.bg.copy(alpha = 0.82f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}
