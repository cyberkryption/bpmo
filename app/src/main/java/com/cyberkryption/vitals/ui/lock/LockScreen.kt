package com.cyberkryption.vitals.ui.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.data.RelockTimeout
import com.cyberkryption.vitals.security.Biometrics
import com.cyberkryption.vitals.ui.components.OutlineButton
import com.cyberkryption.vitals.ui.components.registrationMarks
import com.cyberkryption.vitals.ui.findActivity
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

private enum class LockStatus { Idle, Scanning, Failed }

private const val MAX_BIOMETRIC_ATTEMPTS = 3

/**
 * Drives the system BiometricPrompt. Cancel keeps the app locked; three failed attempts fall
 * back to the device PIN, pattern or password.
 */
private class Unlocker(
    activity: FragmentActivity,
    private val title: String,
    private val subtitle: String,
    private val confirmCredential: (Intent) -> Unit,
    private val onUnlocked: () -> Unit,
) {
    var status by mutableStateOf(LockStatus.Idle)
        private set
    var failures by mutableIntStateOf(0)
        private set

    private var credentialPending = false
    private var promptShowing = false
    private val keyguard = activity.getSystemService(KeyguardManager::class.java)

    // Typed explicitly: the callback below refers back to this property.
    private val prompt: BiometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                promptShowing = false
                failures = 0
                onUnlocked()
            }

            override fun onAuthenticationFailed() {
                failures += 1
                if (failures >= MAX_BIOMETRIC_ATTEMPTS) {
                    credentialPending = true
                    prompt.cancelAuthentication()
                } else {
                    status = LockStatus.Failed
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                promptShowing = false
                when (errorCode) {
                    // Nothing enrolled any more: skip the lock rather than shut the user out.
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                    -> onUnlocked()
                    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> useCredential()
                    else -> if (credentialPending) {
                        useCredential()
                    } else {
                        status = if (failures > 0) LockStatus.Failed else LockStatus.Idle
                    }
                }
            }
        },
    )

    fun authenticate() {
        status = LockStatus.Scanning
        promptShowing = true
        prompt.authenticate(promptInfo(Biometrics.authenticators))
    }

    /** Device credential on its own; before API 30 that goes through the keyguard. */
    fun useCredential() {
        credentialPending = false
        failures = 0
        status = LockStatus.Scanning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptShowing = true
            prompt.authenticate(promptInfo(DEVICE_CREDENTIAL))
        } else {
            @Suppress("DEPRECATION")
            val intent = keyguard?.createConfirmDeviceCredentialIntent(title, subtitle)
            if (intent != null) confirmCredential(intent) else onUnlocked()
        }
    }

    fun switchToCredential() {
        if (promptShowing) {
            // The cancel comes back through onAuthenticationError, which opens the credential prompt.
            credentialPending = true
            prompt.cancelAuthentication()
        } else {
            useCredential()
        }
    }

    fun onCredentialResult(confirmed: Boolean) {
        if (confirmed) onUnlocked() else status = LockStatus.Idle
    }

    private fun promptInfo(authenticators: Int) = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(authenticators)
        .build()
}

@Composable
fun LockScreen(relockTimeout: RelockTimeout, onUnlocked: () -> Unit) {
    val activity = LocalContext.current.findActivity() as FragmentActivity
    val unlocked by rememberUpdatedState(onUnlocked)
    var credentialResult by remember { mutableStateOf<Boolean?>(null) }
    val credentialLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        credentialResult = it.resultCode == Activity.RESULT_OK
    }
    val title = stringResource(R.string.lock_prompt_title)
    val subtitle = stringResource(R.string.lock_prompt_subtitle)
    val u = remember(activity) {
        Unlocker(activity, title, subtitle, confirmCredential = { credentialLauncher.launch(it) }, onUnlocked = { unlocked() })
    }
    LaunchedEffect(credentialResult) {
        credentialResult?.let {
            credentialResult = null
            u.onCredentialResult(it)
        }
    }

    LaunchedEffect(u) { u.authenticate() }

    LockContent(
        status = u.status,
        attemptsLeft = MAX_BIOMETRIC_ATTEMPTS - u.failures,
        relockTimeout = relockTimeout,
        onSensor = u::authenticate,
        onUseCredential = u::switchToCredential,
    )
}

@Composable
private fun LockContent(
    status: LockStatus,
    attemptsLeft: Int,
    relockTimeout: RelockTimeout,
    onSensor: () -> Unit,
    onUseCredential: () -> Unit,
) {
    val c = Vitals.colors
    val ring = if (status == LockStatus.Failed) c.danger.base else c.accent
    val (message, detail) = when (status) {
        LockStatus.Idle -> stringResource(R.string.lock_idle) to stringResource(R.string.lock_idle_detail)
        LockStatus.Scanning -> stringResource(R.string.lock_scanning) to stringResource(R.string.lock_scanning_detail)
        LockStatus.Failed -> stringResource(R.string.lock_failed) to
            pluralStringResource(R.plurals.lock_attempts_left, attemptsLeft, attemptsLeft)
    }
    val messageColor = when (status) {
        LockStatus.Idle -> c.text
        LockStatus.Scanning -> c.accent700
        LockStatus.Failed -> c.danger.ink
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .border(1.dp, c.divider)
                    .registrationMarks(c.mark),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_activity), null, tint = c.accent, modifier = Modifier.size(30.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.lock_title), style = VitalsType.lockTitle, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.lock_body),
                    style = VitalsType.bodyLarge,
                    color = c.text(0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }
            SensorButton(ringColor = ring, scanning = status == LockStatus.Scanning, onClick = onSensor)
            Column(
                Modifier.heightIn(min = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(message, style = VitalsType.status, color = messageColor)
                Text(detail, style = VitalsType.control, color = c.text(0.6f))
            }
        }
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton(stringResource(R.string.lock_use_credential), onUseCredential, Modifier.fillMaxWidth())
            Text(
                stringResource(
                    when (relockTimeout) {
                        RelockTimeout.IMMEDIATELY -> R.string.lock_relock_immediately
                        RelockTimeout.ONE_MINUTE -> R.string.lock_relock_one_minute
                        RelockTimeout.FIVE_MINUTES -> R.string.lock_relock_five_minutes
                    },
                ),
                style = VitalsType.meta,
                color = c.text(0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 112dp square sensor target; a 2dp scan line sweeps across it while the prompt is up. */
@Composable
private fun SensorButton(ringColor: Color, scanning: Boolean, onClick: () -> Unit) {
    val c = Vitals.colors
    val sweep = rememberInfiniteTransition(label = "scan")
    val position by sweep.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanLine",
    )
    Box(
        Modifier
            .size(112.dp)
            .border(1.5.dp, ringColor)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.lock_sensor), onClick = onClick)
            .drawWithContent {
                drawContent()
                if (scanning) {
                    val y = size.height * position
                    drawLine(c.accent, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_fingerprint),
            contentDescription = stringResource(R.string.lock_sensor),
            tint = ringColor,
            modifier = Modifier.size(56.dp),
        )
    }
}
