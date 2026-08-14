/*
 * Copyright (c) 2026 Open Apps Labs
 *
 * This file is part of Jotter
 * * Jotter is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Jotter is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Jotter.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.openappslabs.jotter.data.repository

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.openappslabs.jotter.ui.components.SortDirection
import com.openappslabs.jotter.ui.components.SortType
import com.openappslabs.jotter.data.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

@Immutable
data class UserPreferences(
    val isGridView: Boolean = false,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val isTrueBlackEnabled: Boolean = false,
    val isDynamicColor: Boolean = true,
    val defaultOpenInEdit: Boolean = false,
    val isHapticEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isSecureMode: Boolean = false,
    val showAddCategoryButton: Boolean = true,
    val showSortButton: Boolean = false,
    val is24HourFormat: Boolean = false,
    val dateFormat: String = "dd MMM",
    val sortType: String = SortType.ALPHABETICAL.name,
    val sortDirection: String = SortDirection.ASCENDING.name,
    val fontSizeScale: Float = 1f,
    val lineSpacingScale: Float = 1f,
    val accentColor: String = "",
    val isHomeFocusMode: Boolean = false,
    val streakDays: Int = 0,
    val lastActiveDate: String = ""
)

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val APP_THEME = stringPreferencesKey("app_theme")
        val IS_TRUE_BLACK = booleanPreferencesKey("is_true_black")
        val IS_DYNAMIC_COLOR = booleanPreferencesKey("is_dynamic_color")
        val DEFAULT_OPEN_EDIT = booleanPreferencesKey("default_open_edit")
        val IS_HAPTIC = booleanPreferencesKey("is_haptic")
        val IS_BIOMETRIC = booleanPreferencesKey("is_biometric")
        val IS_APP_LOCK = booleanPreferencesKey("is_app_lock")
        val IS_SECURE_MODE = booleanPreferencesKey("is_secure_mode")
        val SHOW_ADD_CATEGORY_BUTTON = booleanPreferencesKey("show_add_category_button")
        val SHOW_SORT_BUTTON = booleanPreferencesKey("show_sort_button")
        val IS_24_HOUR_FORMAT = booleanPreferencesKey("is_24_hour_format")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val SORT_TYPE = stringPreferencesKey("sort_type")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
        val LINE_SPACING_SCALE = floatPreferencesKey("line_spacing_scale")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val IS_HOME_FOCUS_MODE = booleanPreferencesKey("is_home_focus_mode")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
    }
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                isGridView = preferences[Keys.IS_GRID_VIEW] ?: false,
                appTheme = AppTheme.valueOf(
                    preferences[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
                ),
                isTrueBlackEnabled = preferences[Keys.IS_TRUE_BLACK] ?: false,
                isDynamicColor = preferences[Keys.IS_DYNAMIC_COLOR] ?: true,
                defaultOpenInEdit = preferences[Keys.DEFAULT_OPEN_EDIT] ?: false,
                isHapticEnabled = preferences[Keys.IS_HAPTIC] ?: true,
                isBiometricEnabled = preferences[Keys.IS_BIOMETRIC] ?: false,
                isAppLockEnabled = preferences[Keys.IS_APP_LOCK] ?: false,
                isSecureMode = preferences[Keys.IS_SECURE_MODE] ?: false,
                showAddCategoryButton = preferences[Keys.SHOW_ADD_CATEGORY_BUTTON] ?: true,
                showSortButton = preferences[Keys.SHOW_SORT_BUTTON] ?: false,
                is24HourFormat = preferences[Keys.IS_24_HOUR_FORMAT] ?: false,
                dateFormat = preferences[Keys.DATE_FORMAT] ?: "dd MMM",
                sortType = preferences[Keys.SORT_TYPE] ?: SortType.ALPHABETICAL.name,
                sortDirection = preferences[Keys.SORT_DIRECTION] ?: SortDirection.ASCENDING.name,
                fontSizeScale = preferences[Keys.FONT_SIZE_SCALE] ?: 1f,
                lineSpacingScale = preferences[Keys.LINE_SPACING_SCALE] ?: 1f,
                accentColor = preferences[Keys.ACCENT_COLOR] ?: "",
                isHomeFocusMode = preferences[Keys.IS_HOME_FOCUS_MODE] ?: false,
                streakDays = preferences[Keys.STREAK_DAYS] ?: 0,
                lastActiveDate = preferences[Keys.LAST_ACTIVE_DATE] ?: ""
            )
        }
        .distinctUntilChanged()

    suspend fun setGridView(isGrid: Boolean) {
        dataStore.edit { it[Keys.IS_GRID_VIEW] = isGrid }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    suspend fun setTrueBlack(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_TRUE_BLACK] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultOpenInEdit(enabled: Boolean) {
        dataStore.edit { it[Keys.DEFAULT_OPEN_EDIT] = enabled }
    }

    suspend fun setHaptic(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_HAPTIC] = enabled }
    }

    suspend fun setBiometric(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_BIOMETRIC] = enabled }
    }

    suspend fun setAppLock(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_APP_LOCK] = enabled }
    }

    suspend fun setSecureMode(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_SECURE_MODE] = enabled }
    }

    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            val keepAddButton = preferences[Keys.SHOW_ADD_CATEGORY_BUTTON] ?: true
            preferences.clear()
            preferences[Keys.SHOW_ADD_CATEGORY_BUTTON] = keepAddButton
        }
    }

    suspend fun setShowAddCategoryButton(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_ADD_CATEGORY_BUTTON] = show }
    }

    suspend fun setShowSortButton(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_SORT_BUTTON] = show }
    }

    suspend fun setTimeFormat(is24Hour: Boolean) {
        dataStore.edit { it[Keys.IS_24_HOUR_FORMAT] = is24Hour }
    }

    suspend fun setDateFormat(format: String) {
        dataStore.edit { it[Keys.DATE_FORMAT] = format }
    }

    suspend fun setSortType(sortType: String) {
        dataStore.edit { it[Keys.SORT_TYPE] = sortType }
    }

    suspend fun setSortDirection(sortDirection: String) {
        dataStore.edit { it[Keys.SORT_DIRECTION] = sortDirection }
    }

    suspend fun setFontSizeScale(scale: Float) {
        dataStore.edit { it[Keys.FONT_SIZE_SCALE] = scale }
    }

    suspend fun setLineSpacingScale(scale: Float) {
        dataStore.edit { it[Keys.LINE_SPACING_SCALE] = scale }
    }

    suspend fun setAccentColor(color: String) {
        dataStore.edit { it[Keys.ACCENT_COLOR] = color }
    }

    suspend fun setHomeFocusMode(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_HOME_FOCUS_MODE] = enabled }
    }

    suspend fun recordActiveDay() {
        dataStore.edit { preferences ->
            val today = LocalDate.now().toString()
            val lastActive = preferences[Keys.LAST_ACTIVE_DATE]
            if (lastActive == today) return@edit
            val currentStreak = preferences[Keys.STREAK_DAYS] ?: 0
            val newStreak = if (lastActive == LocalDate.now().minusDays(1).toString()) {
                currentStreak + 1
            } else {
                1
            }
            preferences[Keys.LAST_ACTIVE_DATE] = today
            preferences[Keys.STREAK_DAYS] = newStreak
        }
    }
}