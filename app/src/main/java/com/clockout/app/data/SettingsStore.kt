package com.clockout.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clockout.app.domain.AppSettings
import com.clockout.app.domain.AppThemeStyle
import com.clockout.app.domain.AppFontStyle
import com.clockout.app.domain.ClockInRangeDefaults
import com.clockout.app.domain.LunchMode
import com.clockout.app.domain.LunchDurationLimits
import com.clockout.app.domain.LunchInputStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "clockout_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val workMinutes = intPreferencesKey("work_minutes")
        val clockInRangeEnabled = booleanPreferencesKey("clock_in_range_enabled")
        val clockInStartMinute = intPreferencesKey("clock_in_start_minute")
        val clockInEndMinute = intPreferencesKey("clock_in_end_minute")
        val lunchModeActual = booleanPreferencesKey("lunch_mode_actual")
        val lunchMinutes = intPreferencesKey("lunch_minutes")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderLead = intPreferencesKey("reminder_lead")
        val haptics = booleanPreferencesKey("haptics")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val showLunchControls = booleanPreferencesKey("show_lunch_controls")
        val lunchInputStyle = stringPreferencesKey("lunch_input_style")
        val themeStyle = stringPreferencesKey("theme_style")
        val fontStyle = stringPreferencesKey("font_style")
        val defaultsVersion = intPreferencesKey("defaults_version")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        val rawStart = (p[Keys.clockInStartMinute] ?: ClockInRangeDefaults.START_MINUTE).coerceIn(0, 1439)
        val rawEnd = (p[Keys.clockInEndMinute] ?: ClockInRangeDefaults.END_MINUTE).coerceIn(0, 1439)
        val (clockInStart, clockInEnd) = if (rawStart < rawEnd) rawStart to rawEnd
        else ClockInRangeDefaults.START_MINUTE to ClockInRangeDefaults.END_MINUTE
        AppSettings(
            workMinutes = p[Keys.workMinutes] ?: 480,
            clockInRangeEnabled = p[Keys.clockInRangeEnabled] ?: true,
            clockInStartMinute = clockInStart,
            clockInEndMinute = clockInEnd,
            lunchMode = if (p[Keys.lunchModeActual] == false) LunchMode.FIXED else LunchMode.ACTUAL,
            lunchMinutes = (p[Keys.lunchMinutes] ?: LunchDurationLimits.DEFAULT_MINUTES).coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES),
            reminderEnabled = p[Keys.reminderEnabled] ?: false,
            reminderLeadMinutes = p[Keys.reminderLead] ?: 10,
            hapticsEnabled = p[Keys.haptics] ?: true,
            use24Hour = p[Keys.use24Hour] ?: true,
            showLunchControls = p[Keys.showLunchControls] ?: true,
            lunchInputStyle = p[Keys.lunchInputStyle]?.let { saved ->
                LunchInputStyle.entries.firstOrNull { it.name == saved }
            } ?: LunchInputStyle.QUICK,
            themeStyle = p[Keys.themeStyle]?.let { saved ->
                AppThemeStyle.entries.firstOrNull { it.name == saved }
            } ?: AppThemeStyle.SILVER,
            fontStyle = p[Keys.fontStyle]?.let { saved -> AppFontStyle.entries.firstOrNull { it.name == saved } } ?: AppFontStyle.SYSTEM,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val rawStart = (p[Keys.clockInStartMinute] ?: ClockInRangeDefaults.START_MINUTE).coerceIn(0, 1439)
            val rawEnd = (p[Keys.clockInEndMinute] ?: ClockInRangeDefaults.END_MINUTE).coerceIn(0, 1439)
            val (clockInStart, clockInEnd) = if (rawStart < rawEnd) rawStart to rawEnd
            else ClockInRangeDefaults.START_MINUTE to ClockInRangeDefaults.END_MINUTE
            val old = AppSettings(
                workMinutes = p[Keys.workMinutes] ?: 480,
                clockInRangeEnabled = p[Keys.clockInRangeEnabled] ?: true,
                clockInStartMinute = clockInStart,
                clockInEndMinute = clockInEnd,
                lunchMode = if (p[Keys.lunchModeActual] == false) LunchMode.FIXED else LunchMode.ACTUAL,
                lunchMinutes = (p[Keys.lunchMinutes] ?: LunchDurationLimits.DEFAULT_MINUTES).coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES),
                reminderEnabled = p[Keys.reminderEnabled] ?: false,
                reminderLeadMinutes = p[Keys.reminderLead] ?: 10,
                hapticsEnabled = p[Keys.haptics] ?: true,
                use24Hour = p[Keys.use24Hour] ?: true,
                showLunchControls = p[Keys.showLunchControls] ?: true,
                lunchInputStyle = p[Keys.lunchInputStyle]?.let { saved -> LunchInputStyle.entries.firstOrNull { it.name == saved } }
                    ?: LunchInputStyle.QUICK,
                themeStyle = p[Keys.themeStyle]?.let { saved -> AppThemeStyle.entries.firstOrNull { it.name == saved } }
                    ?: AppThemeStyle.SILVER,
                fontStyle = p[Keys.fontStyle]?.let { saved -> AppFontStyle.entries.firstOrNull { it.name == saved } } ?: AppFontStyle.SYSTEM,
            )
            val next = transform(old)
            p[Keys.workMinutes] = next.workMinutes
            val validStart = next.clockInStartMinute.coerceIn(0, 1439)
            val validEnd = next.clockInEndMinute.coerceIn(0, 1439)
            p[Keys.clockInRangeEnabled] = next.clockInRangeEnabled
            p[Keys.clockInStartMinute] = if (validStart < validEnd) validStart else ClockInRangeDefaults.START_MINUTE
            p[Keys.clockInEndMinute] = if (validStart < validEnd) validEnd else ClockInRangeDefaults.END_MINUTE
            p[Keys.lunchModeActual] = next.lunchMode == LunchMode.ACTUAL
            p[Keys.lunchMinutes] = next.lunchMinutes.coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES)
            p[Keys.reminderEnabled] = next.reminderEnabled
            p[Keys.reminderLead] = next.reminderLeadMinutes
            p[Keys.haptics] = next.hapticsEnabled
            p[Keys.use24Hour] = next.use24Hour
            p[Keys.showLunchControls] = next.showLunchControls
            p[Keys.lunchInputStyle] = next.lunchInputStyle.name
            p[Keys.themeStyle] = next.themeStyle.name
            p[Keys.fontStyle] = next.fontStyle.name
            p[Keys.defaultsVersion] = CURRENT_DEFAULTS_VERSION
        }
    }

    suspend fun migrateV2Defaults(): Boolean {
        var migratedLegacyLunch = false
        context.settingsDataStore.edit { p ->
            if ((p[Keys.defaultsVersion] ?: 1) < CURRENT_DEFAULTS_VERSION) {
                val savedLunch = p[Keys.lunchMinutes]
                if (savedLunch == null || savedLunch == LEGACY_DEFAULT_LUNCH_MINUTES) {
                    p[Keys.lunchMinutes] = LunchDurationLimits.DEFAULT_MINUTES
                    migratedLegacyLunch = savedLunch == LEGACY_DEFAULT_LUNCH_MINUTES
                }
                p[Keys.defaultsVersion] = CURRENT_DEFAULTS_VERSION
            }
        }
        return migratedLegacyLunch
    }

    suspend fun clear() = context.settingsDataStore.edit { it.clear() }

    private companion object {
        const val CURRENT_DEFAULTS_VERSION = 2
        const val LEGACY_DEFAULT_LUNCH_MINUTES = 60
    }
}
