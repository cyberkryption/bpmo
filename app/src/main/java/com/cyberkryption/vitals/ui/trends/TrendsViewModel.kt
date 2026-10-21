package com.cyberkryption.vitals.ui.trends

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.core.Daily
import com.cyberkryption.vitals.core.DayPoint
import com.cyberkryption.vitals.core.Guideline
import com.cyberkryption.vitals.core.Period
import com.cyberkryption.vitals.core.Span
import com.cyberkryption.vitals.core.Status
import com.cyberkryption.vitals.core.VitalsSummary
import com.cyberkryption.vitals.core.categoryCounts
import com.cyberkryption.vitals.data.ReadingRepository
import com.cyberkryption.vitals.data.SettingsRepository
import com.cyberkryption.vitals.export.Exporter
import com.cyberkryption.vitals.security.AppLock
import com.cyberkryption.vitals.ui.container
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate

data class TrendsUiState(
    val span: Span,
    val periodsBack: Int,
    val period: Period,
    val days: List<DayPoint>,
    val summary: VitalsSummary,
    /** What the categories, bar colours and threshold lines follow. */
    val guideline: Guideline,
    val categories: List<Pair<Status, Int>>,
) {
    val isCurrent: Boolean get() = periodsBack == 0
}

class TrendsViewModel(
    private val readings: ReadingRepository,
    settings: SettingsRepository,
    private val exporter: Exporter,
    private val appLock: AppLock,
    private val clock: Clock,
) : ViewModel() {
    private val selection = MutableStateFlow(Span.WEEK to 0)
    private val shares = Channel<Intent>(Channel.BUFFERED)

    /** Share-sheet intents, ready to start. */
    val shareRequests: Flow<Intent> = shares.receiveAsFlow()

    private val guideline = settings.settings.map { it.guideline }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TrendsUiState?> = selection.flatMapLatest { (span, back) ->
        val period = Period.of(span, back, LocalDate.now(clock))
        readings.observeBetween(period.startInstant(clock.zone), period.endInstant(clock.zone)).combine(guideline) { list, chosen ->
            TrendsUiState(
                span, back, period, Daily.points(list, period, clock.zone), VitalsSummary.of(list), chosen,
                categoryCounts(list, chosen),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setSpan(span: Span) {
        selection.value = span to 0
    }

    fun previous() {
        selection.update { (span, back) -> span to back + 1 }
    }

    fun next() {
        selection.update { (span, back) -> span to (back - 1).coerceAtLeast(0) }
    }

    fun exportCsv() = share("text/csv") { exporter.csv(it.guideline) }

    /** A PDF report of the readings from [period]'s first day to its last. */
    fun exportReport(period: Period) = share("application/pdf") { exporter.report(period, it.guideline) }

    private fun share(mimeType: String, write: suspend (TrendsUiState) -> Uri) {
        val s = state.value ?: return
        viewModelScope.launch {
            try {
                val uri = write(s)
                appLock.expectHandoff()
                shares.send(exporter.shareIntent(uri, mimeType))
            } catch (e: IOException) {
                // Nothing to share; the menu simply closes.
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = container
                TrendsViewModel(app.readings, app.settings, app.exporter, app.appLock, app.clock)
            }
        }
    }
}
