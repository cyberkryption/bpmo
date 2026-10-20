package com.cyberkryption.vitals.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.FieldError
import com.cyberkryption.vitals.core.NoteChips
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.ReviewForm
import com.cyberkryption.vitals.core.Validation
import com.cyberkryption.vitals.extraction.ExifTag
import com.cyberkryption.vitals.extraction.OcrResult
import com.cyberkryption.vitals.ui.components.AppBar
import com.cyberkryption.vitals.ui.components.BlueprintFrame
import com.cyberkryption.vitals.ui.components.ChipStyle
import com.cyberkryption.vitals.ui.components.ChoiceChip
import com.cyberkryption.vitals.ui.components.DialogText
import com.cyberkryption.vitals.ui.components.OutlineButton
import com.cyberkryption.vitals.ui.components.PrimaryButton
import com.cyberkryption.vitals.ui.components.Segmented
import com.cyberkryption.vitals.ui.components.StatusTag
import com.cyberkryption.vitals.ui.components.TextAction
import com.cyberkryption.vitals.ui.components.VitalsDialog
import com.cyberkryption.vitals.ui.components.bottomRule
import com.cyberkryption.vitals.ui.components.topRule
import com.cyberkryption.vitals.ui.fieldLabel
import com.cyberkryption.vitals.ui.rememberDateFormats
import com.cyberkryption.vitals.ui.theme.ToneColors
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType
import kotlin.math.roundToInt

@Composable
fun ReviewScreen(
    onDone: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val c = Vitals.colors

    LaunchedEffect(finished) { if (finished) onDone() }
    BackHandler { viewModel.discard() }

    val s = state ?: return
    var picking by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.bg).imePadding()) {
        AppBar(
            title = stringResource(if (s.isEdit) R.string.review_edit_title else R.string.review_title),
            onBack = viewModel::discard,
        ) {
            if (!s.isEdit) {
                Text(
                    stringResource(R.string.review_counter, s.index + 1, s.count),
                    style = VitalsType.action.copy(fontFeatureSettings = "tnum"),
                    color = c.accent700,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
        if (!s.isEdit) Progress(s.index, s.count)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PhotoWithDetections(s.item.photoUri.toString(), s.ocr, s.form, reading = s.form == null)
            val form = s.form ?: return@Column
            Segmented(
                options = listOf(
                    ReadingType.BP to stringResource(R.string.metric_blood_pressure),
                    ReadingType.OXIMETER to stringResource(R.string.type_oximeter),
                ),
                selected = form.type,
                onSelect = viewModel::setType,
                height = 44.dp,
                fillWidth = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ValueFields(form, viewModel::setInput)
            Warnings(form, isEdit = s.isEdit, hadOcr = s.ocr?.boxes?.isNotEmpty() == true)
            form.status?.let { status ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.review_category), style = VitalsType.meta, color = c.text(0.7f))
                    StatusTag(status, long = true)
                }
            }
            TimestampRow(form, s.item.exifTag, onChange = { picking = true })
            NoteEditor(form, viewModel::setNote, viewModel::toggleChip)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .topRule(c.divider)
                .background(c.bg)
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlineButton(
                text = stringResource(if (s.isEdit) R.string.action_cancel else R.string.action_skip),
                onClick = { if (s.isEdit) viewModel.discard() else viewModel.skip() },
            )
            PrimaryButton(
                text = stringResource(if (s.isLast) R.string.action_save else R.string.action_save_next),
                onClick = viewModel::save,
                icon = R.drawable.ic_check,
                enabled = s.form?.canSave == true && !s.saving,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (picking) {
        DateTimePicker(
            initial = s.form?.takenAt,
            onDismiss = { picking = false },
            onPicked = {
                picking = false
                viewModel.setTakenAt(it)
            },
        )
    }

    if (s.showCrisisAdvice) {
        VitalsDialog(
            title = stringResource(R.string.crisis_title),
            onDismissRequest = {},
            confirmText = stringResource(R.string.crisis_save),
            onConfirm = { viewModel.answerCrisisAdvice(save = true) },
            dismissText = stringResource(R.string.crisis_check),
            onDismiss = { viewModel.answerCrisisAdvice(save = false) },
            confirmColor = c.error.ink,
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            DialogText(stringResource(R.string.crisis_body))
        }
    }
}

@Composable
private fun Progress(index: Int, count: Int) {
    val c = Vitals.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) { i ->
            Box(Modifier.weight(1f).height(3.dp).background(if (i <= index) c.accent else c.divider))
        }
    }
}

/** The photo, fitted into a 4:3 frame, with boxes over the regions each value was read from. */
@Composable
private fun PhotoWithDetections(uri: String, ocr: OcrResult?, form: ReviewForm?, reading: Boolean) {
    val c = Vitals.colors
    BlueprintFrame(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
        AsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.detail_photo),
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize().padding(1.dp),
        )
        if (ocr != null && form != null && ocr.width > 0 && ocr.height > 0) {
            BoxWithConstraints(Modifier.matchParentSize()) {
                val scale = minOf(maxWidth.value / ocr.width, maxHeight.value / ocr.height)
                val dx = (maxWidth.value - ocr.width * scale) / 2
                val dy = (maxHeight.value - ocr.height * scale) / 2
                form.fields.forEach { field ->
                    val detection = form.detections[field] ?: return@forEach
                    val b = detection.bounds ?: return@forEach
                    val tone = if (detection.isLowConfidence) c.warning.base else c.accent
                    DetectionBox(
                        label = stringResource(boxLabel(field)),
                        x = (dx + b.left * scale).dp,
                        y = (dy + b.top * scale).dp,
                        width = (b.width * scale).dp,
                        height = (b.height * scale).dp,
                        color = tone,
                        labelColor = if (detection.isLowConfidence) Color.White else c.bg,
                        dashed = detection.isLowConfidence,
                    )
                }
            }
        }
        if (reading) {
            Text(
                stringResource(R.string.review_reading_display),
                style = VitalsType.control,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(c.bg.copy(alpha = 0.85f))
                    .border(1.dp, c.divider)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DetectionBox(
    label: String,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    color: Color,
    labelColor: Color,
    dashed: Boolean,
) {
    Box(
        Modifier
            .offset(x, y)
            .size(width, height)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRect(
                    color = color,
                    style = Stroke(
                        width = stroke,
                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
                    ),
                )
            },
    )
    Text(
        label,
        style = VitalsType.tag.copy(letterSpacing = VitalsType.overline.letterSpacing),
        color = labelColor,
        modifier = Modifier
            .offset(x, (y - 18.dp).coerceAtLeast(0.dp))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

private fun boxLabel(field: Field): Int = when (field) {
    Field.SYSTOLIC -> R.string.box_sys
    Field.DIASTOLIC -> R.string.box_dia
    Field.PULSE -> R.string.box_pul
    Field.SPO2 -> R.string.box_spo2
}

@Composable
private fun ValueFields(form: ReviewForm, onInput: (Field, String) -> Unit) {
    val errors = form.errors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        form.fields.forEachIndexed { i, field ->
            ValueField(
                field = field,
                form = form,
                error = errors[field]?.takeIf { it != FieldError.MISSING },
                onValueChange = { onInput(field, it) },
                imeAction = if (i == form.fields.lastIndex) ImeAction.Done else ImeAction.Next,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 56dp numeric field; flagged when low confidence, red-bordered when out of range. */
@Composable
private fun ValueField(
    field: Field,
    form: ReviewForm,
    error: FieldError?,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    val c = Vitals.colors
    val check = form.needsCheck(field)
    val detection = form.detections[field]
    val tone: ToneColors? = when {
        error != null -> c.error
        check -> c.warning
        else -> null
    }
    val unit = stringResource(if (field == Field.PULSE) R.string.unit_bpm else if (field == Field.SPO2) R.string.unit_percent else R.string.unit_mmhg)
    val caption = when {
        error == FieldError.NOT_BELOW_SYSTOLIC -> stringResource(R.string.review_error_below_systolic)
        error == FieldError.OUT_OF_RANGE -> Validation.range(field).let { stringResource(R.string.review_error_range, it.first, it.last) }
        form.isChanged(field) -> stringResource(R.string.review_caption_edited, unit)
        detection != null -> {
            val percent = (detection.confidence * 100).roundToInt()
            if (check) stringResource(R.string.review_caption_check, unit, percent) else stringResource(R.string.review_caption_confidence, unit, percent)
        }
        else -> unit
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(stringResource(fieldLabel(field)), style = VitalsType.meta, color = tone?.ink ?: c.text(0.7f), maxLines = 1)
        BasicTextField(
            value = form.input(field),
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = VitalsType.inputValue.copy(color = c.text),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(if (tone != null) tone.base.copy(alpha = 0.10f).compositeOver(c.surface) else c.surface)
                        .border(if (tone != null) 1.5.dp else 1.dp, tone?.base ?: c.divider)
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { inner() }
            },
        )
        Text(caption, style = VitalsType.caption, color = tone?.ink ?: c.text(0.55f), maxLines = 1)
    }
}

@Composable
private fun Warnings(form: ReviewForm, isEdit: Boolean, hadOcr: Boolean) {
    val c = Vitals.colors
    val message = when {
        form.fieldsToCheck.isNotEmpty() -> {
            val names = form.fieldsToCheck.map { stringResource(fieldLabel(it)) }
            pluralStringResource(R.plurals.review_hard_to_read, names.size, names.joinToString(stringResource(R.string.list_and)))
        }
        !isEdit && form.detections.isEmpty() ->
            stringResource(if (hadOcr) R.string.review_no_values else R.string.review_unreadable)
        else -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.warning.base.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_alert_triangle),
            contentDescription = null,
            tint = c.warning.ink,
            modifier = Modifier.padding(top = 1.dp).size(18.dp),
        )
        Text(message, style = VitalsType.control.copy(lineHeight = VitalsType.control.fontSize * 1.4f), color = c.warning.ink)
    }
}

@Composable
private fun TimestampRow(form: ReviewForm, exifTag: ExifTag?, onChange: () -> Unit) {
    val c = Vitals.colors
    val formats = rememberDateFormats()
    val takenAt = form.takenAt
    val missing = takenAt == null
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (missing) c.warning.base.copy(alpha = 0.10f) else Color.Transparent)
            .topRule(if (missing) c.warning.base else c.divider)
            .bottomRule(if (missing) c.warning.base else c.divider)
            .padding(vertical = 12.dp, horizontal = if (missing) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painterResource(if (missing) R.drawable.ic_alert_triangle else R.drawable.ic_clock),
            contentDescription = null,
            tint = if (missing) c.warning.ink else c.accent,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                if (takenAt != null) formats.format(formats.full, takenAt) else stringResource(R.string.review_no_date),
                style = VitalsType.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (missing) c.warning.ink else c.text,
            )
            val source = when (form.takenAtSource) {
                DateSource.EXIF -> stringResource(
                    if (exifTag == ExifTag.DATE_TIME) R.string.review_source_exif_datetime else R.string.review_source_exif_original,
                )
                DateSource.CAPTURE -> stringResource(R.string.review_source_capture)
                DateSource.MANUAL -> stringResource(R.string.review_source_manual)
                null -> stringResource(R.string.review_source_missing)
            }
            Text(source, style = VitalsType.meta, color = if (missing) c.warning.ink else c.text(0.6f))
        }
        TextAction(stringResource(if (missing) R.string.action_set_date else R.string.action_change), onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteEditor(form: ReviewForm, onNote: (String) -> Unit, onChip: (String) -> Unit) {
    val c = Vitals.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.review_note), style = VitalsType.meta, color = c.text(0.7f))
        BasicTextField(
            value = form.note,
            onValueChange = onNote,
            textStyle = VitalsType.bodyMedium.copy(color = c.text),
            cursorBrush = SolidColor(c.accent),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .border(1.dp, c.divider)
                        .heightIn(min = 76.dp)
                        .padding(10.dp),
                ) {
                    if (form.note.isEmpty()) {
                        Text(stringResource(R.string.review_note_hint), style = VitalsType.bodyMedium, color = c.text(0.45f))
                    }
                    inner()
                }
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stringArrayResource(R.array.note_chips).forEach { chip ->
                ChoiceChip(
                    label = chip,
                    selected = NoteChips.isSelected(form.note, chip),
                    onClick = { onChip(chip) },
                    style = ChipStyle.Toggle,
                )
            }
        }
    }
}
