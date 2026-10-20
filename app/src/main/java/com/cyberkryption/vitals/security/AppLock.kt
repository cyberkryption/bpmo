package com.cyberkryption.vitals.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cyberkryption.vitals.data.AppSettings
import com.cyberkryption.vitals.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LockState { Checking, Locked, Unlocked }

/**
 * Locks on cold start and after the re-lock timeout in the background, when the user has
 * turned the lock on and the device has a biometric or screen lock enrolled.
 */
class AppLock(
    settings: SettingsRepository,
    scope: CoroutineScope,
    private val canAuthenticate: () -> Boolean,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) : DefaultLifecycleObserver {
    private val _state = MutableStateFlow(LockState.Checking)
    val state: StateFlow<LockState> = _state.asStateFlow()

    private var current = AppSettings()
    private var backgroundedAt: Long? = null
    private var handoffAt: Long? = null

    init {
        scope.launch {
            settings.settings.collect { s ->
                current = s
                when {
                    _state.value == LockState.Checking ->
                        _state.value = if (s.requireBiometric && canAuthenticate()) LockState.Locked else LockState.Unlocked
                    !s.requireBiometric -> _state.value = LockState.Unlocked
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = now()
    }

    override fun onStart(owner: LifecycleOwner) {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        val handoff = handoffAt
        handoffAt = null
        if (handoff != null && now() - handoff < HANDOFF_GRACE_MS) return
        if (_state.value == LockState.Unlocked && current.requireBiometric && canAuthenticate() &&
            now() - since >= current.relockTimeout.millis
        ) {
            _state.value = LockState.Locked
        }
    }

    fun unlock() {
        _state.value = LockState.Unlocked
    }

    /** Call before opening the photo picker or share sheet so coming straight back doesn't re-lock. */
    fun expectHandoff() {
        handoffAt = now()
    }

    private companion object {
        const val HANDOFF_GRACE_MS = 5 * 60_000L
    }
}
