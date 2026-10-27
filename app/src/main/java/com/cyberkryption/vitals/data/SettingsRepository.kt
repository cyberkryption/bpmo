package com.cyberkryption.vitals.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyberkryption.vitals.core.Guideline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

enum class RelockTimeout(val millis: Long) { IMMEDIATELY(0), ONE_MINUTE(60_000), FIVE_MINUTES(300_000) }

/** Light or dark, or whatever the phone is set to. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

/** How the blood pressure chart in Trends, and in the PDF report, shows each day. */
enum class BpChart {
    /** A bar from diastolic up to systolic, coloured by category. */
    BARS,

    /** A line for systolic and another for diastolic. */
    LINES,
}

data class AppSettings(
    val requireBiometric: Boolean = false,
    val relockTimeout: RelockTimeout = RelockTimeout.ONE_MINUTE,
    val categoryColors: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** The lock is offered once, after the first saved reading. */
    val lockOffered: Boolean = false,
    val guideline: Guideline = defaultGuideline(),
    val bpChart: BpChart = BpChart.BARS,
)

/** Until one is chosen, NICE on a phone set to the UK and AHA elsewhere. */
fun defaultGuideline(): Guideline = Guideline.forRegion(Locale.getDefault().country)

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                requireBiometric = prefs[REQUIRE_BIOMETRIC] ?: false,
                relockTimeout = prefs[RELOCK_TIMEOUT]?.let { name -> RelockTimeout.entries.firstOrNull { it.name == name } }
                    ?: RelockTimeout.ONE_MINUTE,
                categoryColors = prefs[CATEGORY_COLORS] ?: true,
                themeMode = prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
                lockOffered = prefs[LOCK_OFFERED] ?: false,
                guideline = prefs[GUIDELINE]?.let { name -> Guideline.entries.firstOrNull { it.name == name } } ?: defaultGuideline(),
                bpChart = prefs[BP_CHART]?.let { name -> BpChart.entries.firstOrNull { it.name == name } } ?: BpChart.BARS,
            )
        }

    suspend fun setRequireBiometric(enabled: Boolean) {
        dataStore.edit { it[REQUIRE_BIOMETRIC] = enabled }
    }

    suspend fun setRelockTimeout(timeout: RelockTimeout) {
        dataStore.edit { it[RELOCK_TIMEOUT] = timeout.name }
    }

    suspend fun setCategoryColors(enabled: Boolean) {
        dataStore.edit { it[CATEGORY_COLORS] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setGuideline(guideline: Guideline) {
        dataStore.edit { it[GUIDELINE] = guideline.name }
    }

    suspend fun setBpChart(chart: BpChart) {
        dataStore.edit { it[BP_CHART] = chart.name }
    }

    suspend fun markLockOffered() {
        dataStore.edit { it[LOCK_OFFERED] = true }
    }

    private companion object {
        val REQUIRE_BIOMETRIC = booleanPreferencesKey("require_biometric")
        val RELOCK_TIMEOUT = stringPreferencesKey("relock_timeout")
        val CATEGORY_COLORS = booleanPreferencesKey("category_colors")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LOCK_OFFERED = booleanPreferencesKey("lock_offered")
        val GUIDELINE = stringPreferencesKey("bp_guideline")
        val BP_CHART = stringPreferencesKey("bp_chart")
    }
}
