package com.cyberkryption.vitals.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.core.Period
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.Span
import com.cyberkryption.vitals.core.VitalsSummary
import com.cyberkryption.vitals.data.ReadingRepository
import com.cyberkryption.vitals.data.SettingsRepository
import com.cyberkryption.vitals.ui.container
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class DashboardUiState(
    val today: LocalDate,
    val span: Span,
    val summary: VitalsSummary,
    val latest: List<Reading>,
    /** Offer the biometric lock once, after the first saved reading. */
    val offerLock: Boolean,
)

class DashboardViewModel(
    private val readings: ReadingRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    canAuthenticate: () -> Boolean,
) : ViewModel() {
    private val span = MutableStateFlow(Span.WEEK)

    private val offerLock = combine(readings.observeCount(), settings.settings) { count, s ->
        count > 0 && !s.lockOffered && !s.requireBiometric && canAuthenticate()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DashboardUiState?> = span.flatMapLatest { span ->
        val today = LocalDate.now(clock)
        val window = Period.of(span, 0, today)
        combine(
            readings.observeBetween(window.startInstant(clock.zone), window.endInstant(clock.zone)),
            readings.observeLatest(LATEST_COUNT),
            offerLock,
        ) { inWindow, latest, offer ->
            DashboardUiState(today, span, VitalsSummary.of(inWindow), latest, offer)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setSpan(value: Span) {
        span.value = value
    }

    fun answerLockOffer(enable: Boolean) {
        viewModelScope.launch {
            if (enable) settings.setRequireBiometric(true)
            settings.markLockOffered()
        }
    }

    companion object {
        private const val LATEST_COUNT = 3

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = container
                DashboardViewModel(app.readings, app.settings, app.clock, app.canAuthenticate)
            }
        }
    }
}
