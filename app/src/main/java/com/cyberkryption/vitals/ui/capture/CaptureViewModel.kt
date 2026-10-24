package com.cyberkryption.vitals.ui.capture

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.core.DateSource
import com.cyberkryption.vitals.core.DisplayParser
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.FrameVotes
import com.cyberkryption.vitals.core.ParsedDisplay
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.TextBox
import com.cyberkryption.vitals.core.fields
import com.cyberkryption.vitals.data.PhotoStore
import com.cyberkryption.vitals.review.ReviewItem
import com.cyberkryption.vitals.review.ReviewSessions
import com.cyberkryption.vitals.ui.container
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.time.Clock
import java.time.Instant

data class CaptureUiState(
    val mode: ReadingType = ReadingType.BP,
    /** Values that recent preview frames agree on, once the display is found: some or all of them. */
    val detected: ParsedDisplay? = null,
    val autoCapture: Boolean = true,
    val torch: Boolean = false,
    val capturing: Boolean = false,
    val failed: Boolean = false,
)

class CaptureViewModel(
    private val photos: PhotoStore,
    private val sessions: ReviewSessions,
    private val clock: Clock,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    private val autoShots = Channel<Unit>(Channel.CONFLATED)

    /** Fires when a complete set of values has held steady long enough to auto-capture. */
    val autoCaptureRequests: Flow<Unit> = autoShots.receiveAsFlow()

    private val votes = FrameVotes()
    private var agreed: Map<Field, Int> = emptyMap()
    private var stableSince = 0L

    fun setMode(mode: ReadingType) {
        _state.value = _state.value.copy(mode = mode, detected = null)
        votes.clear()
        agreed = emptyMap()
    }

    fun toggleAuto() {
        _state.value = _state.value.copy(autoCapture = !_state.value.autoCapture)
    }

    fun toggleTorch() {
        _state.value = _state.value.copy(torch = !_state.value.torch)
    }

    /**
     * Called for each analysed preview frame. A value shows once several recent frames read it the
     * same, each value on its own; auto-capture fires once all of them have and they've held for
     * [STABLE_MS].
     */
    fun onFrame(boxes: List<TextBox>) {
        val s = _state.value
        if (s.capturing) return
        val parsed = DisplayParser.parse(boxes, s.mode)
        val nowAgreed = votes.add(parsed.detections.mapValues { it.value.value })
        if (nowAgreed != agreed) {
            agreed = nowAgreed
            stableSince = now()
        }
        // Each value keeps the confidence of the latest frame that read it, but not its box: that's
        // in preview coordinates.
        val shown = s.detected?.detections.orEmpty()
        val detections = nowAgreed.mapNotNull { (field, value) ->
            val latest = parsed.detections[field]?.takeIf { it.value == value }?.copy(bounds = null)
            (latest ?: shown[field]?.takeIf { it.value == value })?.let { field to it }
        }.toMap()
        _state.value = s.copy(detected = ParsedDisplay(s.mode, detections).takeIf { detections.isNotEmpty() })
        val complete = nowAgreed.keys.containsAll(s.mode.fields)
        if (complete && s.autoCapture && now() - stableSince >= STABLE_MS) autoShots.trySend(Unit)
    }

    fun newPhotoFile(): File {
        _state.value = _state.value.copy(capturing = true, failed = false)
        return photos.newFile()
    }

    fun onCaptureFailed(file: File) {
        file.delete()
        votes.clear()
        agreed = emptyMap()
        _state.value = _state.value.copy(capturing = false, failed = true, detected = null)
    }

    /** Queues the captured photo for review and returns the session id. */
    fun onCaptured(file: File): String = sessions.create(
        listOf(
            ReviewItem(
                photoUri = photos.uriFor(file),
                takenAt = Instant.now(clock),
                dateSource = DateSource.CAPTURE,
                typeHint = _state.value.mode,
                live = _state.value.detected,
            ),
        ),
    )

    companion object {
        private const val STABLE_MS = 1_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CaptureViewModel(container.photos, container.reviewSessions, container.clock) }
        }
    }
}
