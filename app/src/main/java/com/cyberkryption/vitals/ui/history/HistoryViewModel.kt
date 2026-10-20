package com.cyberkryption.vitals.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.core.Daily
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.data.ReadingRepository
import com.cyberkryption.vitals.ui.container
import com.cyberkryption.vitals.ui.valueText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

enum class HistoryFilter(val type: ReadingType?) { ALL(null), BP(ReadingType.BP), SPO2(ReadingType.OXIMETER) }

data class DayGroup(val date: LocalDate, val readings: List<Reading>)

data class HistoryUiState(
    val today: LocalDate,
    val filter: HistoryFilter,
    val query: String,
    val groups: List<DayGroup>,
    val hasReadings: Boolean,
)

class HistoryViewModel(readings: ReadingRepository, private val clock: Clock) : ViewModel() {
    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val query = MutableStateFlow("")

    val state: StateFlow<HistoryUiState?> = combine(readings.observeAll(), filter, query) { all, f, q ->
        val visible = all.filter { (f.type == null || it.type == f.type) && it.matches(q) }
        HistoryUiState(
            today = LocalDate.now(clock),
            filter = f,
            query = q,
            groups = Daily.groups(visible, clock.zone).map { (date, list) -> DayGroup(date, list) },
            hasReadings = all.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    private fun Reading.matches(q: String): Boolean {
        val needle = q.trim()
        if (needle.isEmpty()) return true
        return note.orEmpty().contains(needle, ignoreCase = true) || valueText().contains(needle)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(container.readings, container.clock) }
        }
    }
}
