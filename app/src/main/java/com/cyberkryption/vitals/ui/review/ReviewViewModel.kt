package com.cyberkryption.vitals.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.DisplayParser
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.Guideline
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.ReviewForm
import com.cyberkryption.vitals.data.PhotoStore
import com.cyberkryption.vitals.data.ReadingRepository
import com.cyberkryption.vitals.extraction.OcrResult
import com.cyberkryption.vitals.extraction.TextExtractor
import com.cyberkryption.vitals.review.ReviewItem
import com.cyberkryption.vitals.review.ReviewSession
import com.cyberkryption.vitals.review.ReviewSessions
import com.cyberkryption.vitals.ui.container
import com.cyberkryption.vitals.ui.navigation.ReviewRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

data class ReviewUiState(
    val index: Int,
    val count: Int,
    val isEdit: Boolean,
    val item: ReviewItem,
    /** Null while the photo is still being read. */
    val form: ReviewForm? = null,
    val ocr: OcrResult? = null,
    val showCrisisAdvice: Boolean = false,
    val saving: Boolean = false,
) {
    val isLast: Boolean get() = index == count - 1
}

class ReviewViewModel(
    sessionId: String,
    sessions: ReviewSessions,
    private val extractor: TextExtractor,
    private val readings: ReadingRepository,
    private val photos: PhotoStore,
    /** Outlives this screen, so discarded photos are deleted even as it closes. */
    private val appScope: CoroutineScope,
    private val clock: Clock,
    private val onSessionEnd: () -> Unit,
) : ViewModel() {
    private val session: ReviewSession? = sessions.get(sessionId)

    private val _state = MutableStateFlow<ReviewUiState?>(null)
    val state: StateFlow<ReviewUiState?> = _state.asStateFlow()

    private val _finished = MutableStateFlow(session == null)

    /** True once every item is saved or skipped, or the review was abandoned. */
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    init {
        if (session != null) load(0)
    }

    private fun load(index: Int) {
        val session = session ?: return
        val item = session.items[index]
        val base = ReviewUiState(index, session.items.size, session.isEdit, item)
        val existing = item.existing
        if (existing != null) {
            _state.value = base.copy(form = ReviewForm.from(existing))
            return
        }
        _state.value = base
        viewModelScope.launch {
            // An unreadable photo still gets reviewed: the fields just start empty.
            val ocr = try {
                extractor.recognize(item.photoUri, item.typeHint)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            val boxes = ocr?.boxes.orEmpty()
            val fromPhoto = DisplayParser.parse(boxes, item.typeHint ?: DisplayParser.guessType(boxes))
            val live = item.live?.takeIf { it.type == fromPhoto.type && it.detections.size > fromPhoto.detections.size }
            val form = ReviewForm.from(live ?: fromPhoto, item.takenAt, item.dateSource)
            _state.update { if (it?.index == index) it.copy(form = form, ocr = ocr) else it }
        }
    }

    private fun updateForm(transform: (ReviewForm) -> ReviewForm) {
        _state.update { s -> s?.form?.let { s.copy(form = transform(it)) } ?: s }
    }

    fun setType(type: ReadingType) {
        val s = _state.value ?: return
        if (s.form?.type == type) return
        val parsed = DisplayParser.parse(s.ocr?.boxes.orEmpty(), type)
        updateForm { it.withType(parsed) }
    }

    fun setInput(field: Field, text: String) = updateForm { it.withInput(field, text) }

    fun setNote(note: String) = updateForm { it.withNote(note) }

    fun toggleChip(chip: String) = updateForm { it.toggleChip(chip) }

    fun setTakenAt(instant: Instant) = updateForm { it.withTakenAt(instant, DateSource.MANUAL) }

    /** [guideline] is the one the screen shows categories under, so the warning matches the tag. */
    fun save(guideline: Guideline) {
        val s = _state.value ?: return
        val form = s.form ?: return
        if (!form.canSave || s.saving) return
        if (form.status(guideline)?.isUrgent == true) {
            _state.update { it?.copy(showCrisisAdvice = true) }
        } else {
            persist()
        }
    }

    /** The crisis advisory can't be dismissed; the user either saves or goes back to check the values. */
    fun answerCrisisAdvice(save: Boolean) {
        _state.update { it?.copy(showCrisisAdvice = false) }
        if (save) persist()
    }

    private fun persist() {
        val s = _state.value ?: return
        val form = s.form ?: return
        _state.update { it?.copy(saving = true) }
        viewModelScope.launch {
            val existing = s.item.existing
            val reading = if (existing != null) {
                form.toReading(existing.photoUri, existing.createdAt, existing.id, existing.editedFields)
            } else {
                form.toReading(s.item.photoUri.toString(), Instant.now(clock))
            }
            readings.save(reading)
            advance(s.index)
        }
    }

    fun skip() {
        val s = _state.value ?: return
        if (!s.isEdit) deletePhotos(listOf(s.item))
        advance(s.index)
    }

    /** Leaves the review; photos not yet saved are removed from private storage. */
    fun discard() {
        val s = _state.value
        val session = session
        if (s != null && session != null && !s.isEdit) deletePhotos(session.items.drop(s.index))
        finish()
    }

    private fun advance(index: Int) {
        val session = session ?: return
        if (index + 1 < session.items.size) load(index + 1) else finish()
    }

    private fun finish() {
        onSessionEnd()
        _finished.value = true
    }

    private fun deletePhotos(items: List<ReviewItem>) {
        appScope.launch { items.forEach { photos.delete(it.photoUri.toString()) } }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = createSavedStateHandle()
                val sessionId = handle.toRoute<ReviewRoute>().sessionId
                val app = container
                ReviewViewModel(
                    sessionId = sessionId,
                    sessions = app.reviewSessions,
                    extractor = app.extractor,
                    readings = app.readings,
                    photos = app.photos,
                    appScope = app.applicationScope,
                    clock = app.clock,
                    onSessionEnd = { app.reviewSessions.remove(sessionId) },
                )
            }
        }
    }
}
