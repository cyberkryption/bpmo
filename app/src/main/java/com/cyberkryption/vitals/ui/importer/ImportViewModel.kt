package com.cyberkryption.vitals.ui.importer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.data.PhotoStore
import com.cyberkryption.vitals.extraction.ExifReader
import com.cyberkryption.vitals.extraction.PhotoDate
import com.cyberkryption.vitals.review.ReviewItem
import com.cyberkryption.vitals.review.ReviewSessions
import com.cyberkryption.vitals.security.AppLock
import com.cyberkryption.vitals.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

data class ImportPhoto(val uri: Uri, val date: PhotoDate?)

data class ImportGroup(val thisWeek: Boolean, val photos: List<ImportPhoto>)

data class ImportUiState(
    val photos: List<ImportPhoto> = emptyList(),
    /** Selection order is review order. */
    val selected: List<Uri> = emptyList(),
    val loading: Boolean = false,
    val preparing: Boolean = false,
    val failed: Boolean = false,
    /** Set once the photos are copied and queued; the screen moves on to Review. */
    val sessionId: String? = null,
) {
    val undatedSelected: Int get() = photos.count { it.uri in selected && it.date == null }
}

class ImportViewModel(
    private val exif: ExifReader,
    private val photos: PhotoStore,
    private val sessions: ReviewSessions,
    private val appLock: AppLock,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    /** Photos dated in the last seven days, then everything older or undated; newest first. */
    fun groups(state: ImportUiState): List<ImportGroup> {
        val weekStart: Instant = LocalDate.now(clock).minusDays(6).atStartOfDay(clock.zone).toInstant()
        val (recent, earlier) = state.photos.partition { it.date != null && !it.date.instant.isBefore(weekStart) }
        val newestFirst = compareByDescending<ImportPhoto> { it.date?.instant ?: Instant.MIN }
        return listOf(ImportGroup(true, recent.sortedWith(newestFirst)), ImportGroup(false, earlier.sortedWith(newestFirst)))
            .filter { it.photos.isNotEmpty() }
    }

    fun beforePicker() = appLock.expectHandoff()

    fun add(uris: List<Uri>) {
        val fresh = uris.filter { uri -> _state.value.photos.none { it.uri == uri } }
        if (fresh.isEmpty()) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val added = fresh.map { ImportPhoto(it, exif.read(it)) }
            _state.update { s -> s.copy(photos = s.photos + added, selected = s.selected + fresh, loading = false) }
        }
    }

    fun toggle(uri: Uri) {
        _state.update { s -> s.copy(selected = if (uri in s.selected) s.selected - uri else s.selected + uri) }
    }

    /** Copies the selection into private storage, in selection order, and queues it for review. */
    fun review() {
        val s = _state.value
        if (s.selected.isEmpty() || s.preparing) return
        _state.update { it.copy(preparing = true, failed = false) }
        viewModelScope.launch {
            val copied = mutableListOf<ReviewItem>()
            try {
                for (uri in s.selected) {
                    val date = s.photos.first { it.uri == uri }.date
                    copied += ReviewItem(
                        photoUri = photos.importCopy(uri),
                        takenAt = date?.instant,
                        dateSource = if (date != null) DateSource.EXIF else null,
                        exifTag = date?.tag,
                    )
                }
                _state.update { it.copy(preparing = false, sessionId = sessions.create(copied)) }
            } catch (e: IOException) {
                copied.forEach { photos.delete(it.photoUri.toString()) }
                _state.update { it.copy(preparing = false, failed = true) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = container
                ImportViewModel(app.exif, app.photos, app.reviewSessions, app.appLock, app.clock)
            }
        }
    }
}
