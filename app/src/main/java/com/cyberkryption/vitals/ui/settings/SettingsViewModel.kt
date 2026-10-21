package com.cyberkryption.vitals.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.data.AppSettings
import com.cyberkryption.vitals.data.RelockTimeout
import com.cyberkryption.vitals.data.SettingsRepository
import com.cyberkryption.vitals.data.ThemeMode
import com.cyberkryption.vitals.security.AppLock
import com.cyberkryption.vitals.ui.container
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val appLock: AppLock,
    private val canAuthenticate: () -> Boolean,
) : ViewModel() {
    val state: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _canLock = MutableStateFlow(canAuthenticate())

    /** False when no biometric or screen lock is enrolled; the lock can't be turned on. */
    val canLock: StateFlow<Boolean> = _canLock.asStateFlow()

    fun refreshEnrollment() {
        _canLock.value = canAuthenticate()
    }

    fun setRequireBiometric(enabled: Boolean) {
        if (enabled && !canAuthenticate()) return
        viewModelScope.launch {
            settings.setRequireBiometric(enabled)
            settings.markLockOffered()
        }
    }

    fun setRelockTimeout(timeout: RelockTimeout) {
        viewModelScope.launch { settings.setRelockTimeout(timeout) }
    }

    fun setCategoryColors(enabled: Boolean) {
        viewModelScope.launch { settings.setCategoryColors(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun beforeSystemSettings() = appLock.expectHandoff()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.settings, container.appLock, container.canAuthenticate) }
        }
    }
}
