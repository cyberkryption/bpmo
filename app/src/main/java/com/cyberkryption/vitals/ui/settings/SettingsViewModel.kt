package com.cyberkryption.vitals.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyberkryption.vitals.data.AppSettings
import com.cyberkryption.vitals.data.BackupException
import com.cyberkryption.vitals.data.BackupManager
import com.cyberkryption.vitals.data.BackupProblem
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

/** Progress and outcome of a backup or restore, shown until dismissed. */
sealed interface BackupStatus {
    data object BackingUp : BackupStatus

    data object Restoring : BackupStatus

    data class Saved(val count: Int) : BackupStatus

    data class Restored(val added: Int, val skipped: Int) : BackupStatus

    data class Failed(val problem: BackupProblem) : BackupStatus
}

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val appLock: AppLock,
    private val canAuthenticate: () -> Boolean,
    private val backup: BackupManager,
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

    private val _backupStatus = MutableStateFlow<BackupStatus?>(null)
    val backupStatus: StateFlow<BackupStatus?> = _backupStatus.asStateFlow()

    fun backupFileName(): String = backup.fileName()

    fun backUp(target: Uri) = runBackup(BackupStatus.BackingUp) { BackupStatus.Saved(backup.backUp(target)) }

    fun restore(source: Uri) = runBackup(BackupStatus.Restoring) {
        backup.restore(source).let { BackupStatus.Restored(it.added.size, it.skipped) }
    }

    fun dismissBackupStatus() {
        _backupStatus.value = null
    }

    private fun runBackup(working: BackupStatus, block: suspend () -> BackupStatus) {
        if (_backupStatus.value == BackupStatus.BackingUp || _backupStatus.value == BackupStatus.Restoring) return
        _backupStatus.value = working
        viewModelScope.launch {
            _backupStatus.value = try {
                block()
            } catch (e: BackupException) {
                BackupStatus.Failed(e.problem)
            }
        }
    }

    /** System Settings and the file pickers leave the app briefly; don't re-lock for that. */
    fun beforeHandoff() = appLock.expectHandoff()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = container
                SettingsViewModel(app.settings, app.appLock, app.canAuthenticate, app.backup)
            }
        }
    }
}
