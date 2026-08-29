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
import com.clockout.app.domain.LunchMode
import com.clockout.app.domain.LunchDurationLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "clockout_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val workMinutes = intPreferencesKey("work_minutes")
        val lunchModeActual = booleanPreferencesKey("lunch_mode_actual")
        val lunchMinutes = intPreferencesKey("lunch_minutes")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderLead = intPreferencesKey("reminder_lead")
        val haptics = booleanPreferencesKey("haptics")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val showLunchControls = booleanPreferencesKey("show_lunch_controls")
        val themeStyle = stringPreferencesKey("theme_style")
        val fontStyle = stringPreferencesKey("font_style")
        val defaultsVersion = intPreferencesKey("defaults_version")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            workMinutes = p[Keys.workMinutes] ?: 480,
            lunchMode = if (p[Keys.lunchModeActual] == false) LunchMode.FIXED else LunchMode.ACTUAL,
            lunchMinutes = (p[Keys.lunchMinutes] ?: LunchDurationLimits.DEFAULT_MINUTES).coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES),
            reminderEnabled = p[Keys.reminderEnabled] ?: false,
            reminderLeadMinutes = p[Keys.reminderLead] ?: 10,
            hapticsEnabled = p[Keys.haptics] ?: true,
            use24Hour = p[Keys.use24Hour] ?: true,
            showLunchControls = p[Keys.showLunchControls] ?: true,
            themeStyle = p[Keys.themeStyle]?.let { saved ->
                AppThemeStyle.entries.firstOrNull { it.name == saved }
            } ?: AppThemeStyle.SILVER,
            fontStyle = p[Keys.fontStyle]?.let { saved -> AppFontStyle.entries.firstOrNull { it.name == saved } } ?: AppFontStyle.SYSTEM,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val old = AppSettings(
                p[Keys.workMinutes] ?: 480,
                if (p[Keys.lunchModeActual] == false) LunchMode.FIXED else LunchMode.ACTUAL,
                (p[Keys.lunchMinutes] ?: LunchDurationLimits.DEFAULT_MINUTES).coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES),
                p[Keys.reminderEnabled] ?: false,
                p[Keys.reminderLead] ?: 10,
                p[Keys.haptics] ?: true,
                p[Keys.use24Hour] ?: true,
                p[Keys.showLunchControls] ?: true,
                p[Keys.themeStyle]?.let { saved -> AppThemeStyle.entries.firstOrNull { it.name == saved } }
                    ?: AppThemeStyle.SILVER,
                p[Keys.fontStyle]?.let { saved -> AppFontStyle.entries.firstOrNull { it.name == saved } } ?: AppFontStyle.SYSTEM,
            )
            val next = transform(old)
            p[Keys.workMinutes] = next.workMinutes
            p[Keys.lunchModeActual] = next.lunchMode == LunchMode.ACTUAL
            p[Keys.lunchMinutes] = next.lunchMinutes.coerceIn(LunchDurationLimits.MIN_MINUTES, LunchDurationLimits.MAX_MINUTES)
            p[Keys.reminderEnabled] = next.reminderEnabled
            p[Keys.reminderLead] = next.reminderLeadMinutes
            p[Keys.haptics] = next.hapticsEnabled
            p[Keys.use24Hour] = next.use24Hour
            p[Keys.showLunchControls] = next.showLunchControls
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
