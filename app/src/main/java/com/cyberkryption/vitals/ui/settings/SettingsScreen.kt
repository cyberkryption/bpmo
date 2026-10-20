package com.cyberkryption.vitals.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberkryption.vitals.BuildConfig
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.data.RelockTimeout
import com.cyberkryption.vitals.security.Biometrics
import com.cyberkryption.vitals.ui.components.AppBar
import com.cyberkryption.vitals.ui.components.Kicker
import com.cyberkryption.vitals.ui.components.Segmented
import com.cyberkryption.vitals.ui.components.SquareSwitch
import com.cyberkryption.vitals.ui.components.TextAction
import com.cyberkryption.vitals.ui.components.bottomRule
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canLock by viewModel.canLock.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = Vitals.colors

    // Enrollment may change while the user is away in system Settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshEnrollment() }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        AppBar(stringResource(R.string.settings_title), onBack = onBack)
        val s = state ?: return@Column
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section(stringResource(R.string.settings_security)) {
                ToggleRow(
                    title = stringResource(R.string.settings_require_biometric),
                    description = stringResource(R.string.settings_require_biometric_detail),
                    checked = s.requireBiometric,
                    enabled = canLock || s.requireBiometric,
                    onCheckedChange = viewModel::setRequireBiometric,
                )
                if (!canLock) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(c.warning.base.copy(alpha = 0.12f))
                            .padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 4.dp),
                    ) {
                        Text(stringResource(R.string.settings_not_enrolled), style = VitalsType.control, color = c.warning.ink)
                        TextAction(
                            stringResource(R.string.settings_open_system),
                            onClick = {
                                viewModel.beforeSystemSettings()
                                context.startActivity(Biometrics.enrollIntent())
                            },
                            color = c.warning.ink,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
                Column(
                    Modifier
                        .padding(vertical = 14.dp)
                        .alpha(if (s.requireBiometric) 1f else 0.45f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.settings_relock), style = VitalsType.bodyLarge)
                    Segmented(
                        options = listOf(
                            RelockTimeout.IMMEDIATELY to stringResource(R.string.settings_relock_immediately),
                            RelockTimeout.ONE_MINUTE to stringResource(R.string.settings_relock_one_minute),
                            RelockTimeout.FIVE_MINUTES to stringResource(R.string.settings_relock_five_minutes),
                        ),
                        selected = s.relockTimeout,
                        onSelect = { if (s.requireBiometric) viewModel.setRelockTimeout(it) },
                        fillWidth = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Section(stringResource(R.string.settings_display)) {
                ToggleRow(
                    title = stringResource(R.string.settings_category_colors),
                    description = stringResource(R.string.settings_category_colors_detail),
                    checked = s.categoryColors,
                    onCheckedChange = viewModel::setCategoryColors,
                )
            }
            Section(stringResource(R.string.settings_about)) {
                Text(
                    stringResource(R.string.settings_about_body),
                    style = VitalsType.control,
                    color = c.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = VitalsType.meta,
                    color = c.text(0.5f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Kicker(title, Modifier.padding(bottom = 4.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val c = Vitals.colors
    Row(
        Modifier
            .fillMaxWidth()
            .bottomRule(c.hairline)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = VitalsType.bodyLarge)
            Text(description, style = VitalsType.meta, color = c.muted)
        }
        SquareSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
