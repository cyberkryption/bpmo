package com.cyberkryption.vitals.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.data.ReadingRepository
import com.cyberkryption.vitals.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState

    data object Missing : DetailState

    data class Loaded(val reading: Reading) : DetailState
}

class ReadingDetailViewModel(id: Long, private val readings: ReadingRepository) : ViewModel() {
    val state: StateFlow<DetailState> = readings.observe(id)
        .map { if (it == null) DetailState.Missing else DetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    /** The state turns [DetailState.Missing] once the row is gone, which closes the screen. */
    fun delete() {
        val reading = (state.value as? DetailState.Loaded)?.reading ?: return
        viewModelScope.launch { readings.delete(reading) }
    }

    companion object {
        fun factory(id: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { ReadingDetailViewModel(id, container.readings) }
        }
    }
}
