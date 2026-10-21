package com.cyberkryption.vitals.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.fields
import com.cyberkryption.vitals.core.status
import com.cyberkryption.vitals.ui.components.AppBar
import com.cyberkryption.vitals.ui.components.DialogText
import com.cyberkryption.vitals.ui.components.IconAction
import com.cyberkryption.vitals.ui.components.PhotoFrame
import com.cyberkryption.vitals.ui.components.SectionLabel
import com.cyberkryption.vitals.ui.components.StatusBadge
import com.cyberkryption.vitals.ui.components.TypeIcon
import com.cyberkryption.vitals.ui.components.ValueWithUnit
import com.cyberkryption.vitals.ui.components.VitalsCard
import com.cyberkryption.vitals.ui.components.VitalsDialog
import com.cyberkryption.vitals.ui.components.bottomRule
import com.cyberkryption.vitals.ui.fieldLabel
import com.cyberkryption.vitals.ui.rememberDateFormats
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

@Composable
fun ReadingDetailScreen(
    readingId: Long,
    showBack: Boolean,
    onBack: () -> Unit,
    onEdit: (Reading) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReadingDetailViewModel = viewModel(
        key = "reading-$readingId",
        factory = ReadingDetailViewModel.factory(readingId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val c = Vitals.colors
    val reading = (state as? DetailState.Loaded)?.reading

    LaunchedEffect(state) {
        if (state == DetailState.Missing) onDeleted()
    }

    Column(modifier.fillMaxSize().background(c.bg)) {
        AppBar(
            title = stringResource(R.string.detail_title),
            onBack = if (showBack) onBack else null,
            titleStartPadding = 16.dp,
        ) {
            IconAction(R.drawable.ic_pencil, stringResource(R.string.action_edit), { reading?.let(onEdit) }, iconSize = 20.dp, enabled = reading != null)
            IconAction(R.drawable.ic_trash, stringResource(R.string.action_delete), { confirmDelete = true }, iconSize = 20.dp, enabled = reading != null)
        }
        if (reading != null) DetailBody(reading)
    }

    if (confirmDelete) {
        VitalsDialog(
            title = stringResource(R.string.delete_title),
            onDismissRequest = { confirmDelete = false },
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            dismissText = stringResource(R.string.action_cancel),
            confirmColor = c.error.ink,
        ) {
            DialogText(stringResource(R.string.delete_body))
        }
    }
}

@Composable
private fun DetailBody(reading: Reading) {
    val c = Vitals.colors
    val formats = rememberDateFormats()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VitalsCard(
            Modifier.fillMaxWidth(),
            color = lerp(c.surface, c.primaryContainer, if (c.isDark) 0.25f else 0.3f),
            contentPadding = PaddingValues(18.dp),
            spacing = 12.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeIcon(reading.type)
                Text(
                    formats.format(formats.full, reading.takenAt),
                    style = VitalsType.cardTitle,
                    modifier = Modifier.weight(1f),
                )
            }
            ValueWithUnit(
                value = when (reading.type) {
                    ReadingType.BP -> "${reading.systolic} / ${reading.diastolic}"
                    ReadingType.OXIMETER -> reading.spo2.toString()
                },
                unit = stringResource(if (reading.type == ReadingType.BP) R.string.unit_mmhg else R.string.unit_percent),
                valueStyle = VitalsType.detailValue,
            )
            StatusBadge(reading.status, long = true)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(IntrinsicSize.Min),
            ) {
                reading.type.fields.forEachIndexed { i, field ->
                    if (i > 0) Box(Modifier.padding(horizontal = 14.dp).width(1.dp).fillMaxHeight().background(c.divider))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(fieldLabel(field)), style = VitalsType.meta, color = c.muted, maxLines = 1)
                        Text(reading.value(field)?.toString().orEmpty(), style = VitalsType.stripValue)
                    }
                }
            }
        }

        val note = reading.note
        if (!note.isNullOrBlank()) {
            VitalsCard(Modifier.fillMaxWidth(), spacing = 6.dp) {
                SectionLabel(stringResource(R.string.review_note))
                Text(note, style = VitalsType.bodyLarge)
            }
        }

        PhotoFrame(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            AsyncImage(
                model = reading.photoUri,
                contentDescription = stringResource(R.string.detail_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val rows = listOf(
            stringResource(R.string.detail_taken) to formats.format(formats.fullSeconds, reading.takenAt),
            stringResource(R.string.detail_date_source) to stringResource(
                when (reading.takenAtSource) {
                    DateSource.EXIF -> R.string.date_source_exif
                    DateSource.CAPTURE -> R.string.date_source_capture
                    DateSource.MANUAL -> R.string.date_source_manual
                },
            ),
            stringResource(R.string.detail_device) to stringResource(
                if (reading.type == ReadingType.BP) R.string.device_bp else R.string.device_oximeter,
            ),
            stringResource(R.string.detail_edited) to editedText(reading.editedFields),
        )
        VitalsCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp), spacing = 0.dp) {
            MetadataList(rows)
        }
    }
}

@Composable
private fun editedText(fields: Set<Field>): String {
    if (fields.isEmpty()) return stringResource(R.string.detail_not_edited)
    val names = Field.entries.filter { it in fields }.map { stringResource(fieldLabel(it)) }
    return stringResource(R.string.detail_confirmed_manually, names.joinToString(", "))
}

/** Label/value pairs with the label column sized to its widest label. */
@Composable
private fun MetadataList(rows: List<Pair<String, String>>) {
    val c = Vitals.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidth = with(density) { rows.maxOf { measurer.measure(it.first, VitalsType.meta).size.width }.toDp() }
    rows.forEachIndexed { i, (label, value) ->
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (i < rows.lastIndex) Modifier.bottomRule(c.divider) else Modifier)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(label, style = VitalsType.meta, color = c.muted, modifier = Modifier.width(labelWidth))
            Text(value, style = VitalsType.meta, modifier = Modifier.weight(1f))
        }
    }
}
