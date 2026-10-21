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
    /** Values that recent preview frames agree on, once the display is found. */
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
    private var agreed: Map<Field, Int>? = null
    private var stableSince = 0L

    fun setMode(mode: ReadingType) {
        _state.value = _state.value.copy(mode = mode, detected = null)
        votes.clear()
        agreed = null
    }

    fun toggleAuto() {
        _state.value = _state.value.copy(autoCapture = !_state.value.autoCapture)
    }

    fun toggleTorch() {
        _state.value = _state.value.copy(torch = !_state.value.torch)
    }

    /**
     * Called for each analysed preview frame. Values show, and auto-capture can fire, only once
     * several recent frames read the same; then they must hold for [STABLE_MS].
     */
    fun onFrame(boxes: List<TextBox>) {
        val s = _state.value
        if (s.capturing) return
        val parsed = DisplayParser.parse(boxes, s.mode)
        val complete = parsed.detections.keys.containsAll(s.mode.fields)
        val values = parsed.detections.mapValues { it.value.value }.takeIf { complete }
        val nowAgreed = votes.add(values)
        if (nowAgreed != agreed) {
            agreed = nowAgreed
            stableSince = now()
        }
        when {
            nowAgreed == null -> _state.value = s.copy(detected = null)
            // Keeps the frame's confidences, but drops its boxes: they're in preview coordinates.
            values == nowAgreed -> _state.value = s.copy(detected = parsed.withoutBounds())
        }
        if (nowAgreed != null && s.autoCapture && now() - stableSince >= STABLE_MS) autoShots.trySend(Unit)
    }

    fun newPhotoFile(): File {
        _state.value = _state.value.copy(capturing = true, failed = false)
        return photos.newFile()
    }

    fun onCaptureFailed(file: File) {
        file.delete()
        votes.clear()
        agreed = null
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

private fun ParsedDisplay.withoutBounds() = copy(detections = detections.mapValues { it.value.copy(bounds = null) })
