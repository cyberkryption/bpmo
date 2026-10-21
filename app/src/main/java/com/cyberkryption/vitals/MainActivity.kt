package com.cyberkryption.vitals

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.cyberkryption.vitals.data.RelockTimeout
import com.cyberkryption.vitals.data.ThemeMode
import com.cyberkryption.vitals.data.defaultGuideline
import com.cyberkryption.vitals.security.LockState
import com.cyberkryption.vitals.ui.VitalsApp
import com.cyberkryption.vitals.ui.lock.LockScreen
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** A FragmentActivity because BiometricPrompt needs one. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as VitalsApplication).container
        protectWhileLocked(container)
        setContent { VitalsRoot(container, onDarkThemeChange = ::matchSystemBars) }
    }

    /** Status and navigation bar icons follow the app's theme, which may differ from the phone's. */
    private fun matchSystemBars(dark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
        )
    }

    /**
     * FLAG_SECURE while locked. With the lock on, recents get no thumbnail: API 33+ can say so
     * directly; older versions keep FLAG_SECURE on throughout.
     */
    private fun protectWhileLocked(container: AppContainer) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                combine(container.appLock.state, container.settings.settings) { lock, settings ->
                    lock to settings.requireBiometric
                }.collect { (lock, required) ->
                    val legacyRecents = required && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    if (lock != LockState.Unlocked || legacyRecents) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!required)
                }
            }
        }
    }
}

// The scrims enableEdgeToEdge() uses by default for three-button navigation.
private val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@Composable
private fun VitalsRoot(container: AppContainer, onDarkThemeChange: (Boolean) -> Unit) {
    val lock by container.appLock.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    // Hoisted so the back stack, and its ViewModels, survive the lock screen.
    val navController = rememberNavController()
    val dark = (settings?.themeMode ?: ThemeMode.SYSTEM).isDark(isSystemInDarkTheme())
    LaunchedEffect(dark) { onDarkThemeChange(dark) }

    VitalsTheme(
        darkTheme = dark,
        categoryColors = settings?.categoryColors ?: true,
        guideline = settings?.guideline ?: defaultGuideline(),
    ) {
        Box(Modifier.fillMaxSize().background(Vitals.colors.bg)) {
            when (lock) {
                LockState.Checking -> Unit
                LockState.Locked -> LockScreen(
                    relockTimeout = settings?.relockTimeout ?: RelockTimeout.ONE_MINUTE,
                    onUnlocked = container.appLock::unlock,
                )
                LockState.Unlocked -> VitalsApp(container, navController)
            }
        }
    }
}
