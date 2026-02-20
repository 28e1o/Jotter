/*
 * Copyright (c) 2026 Open Apps Labs
 *
 * This file is part of Jotter
 *
 * Jotter is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Jotter is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Jotter.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.openappslabs.jotter.ui.screens.settingsscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openappslabs.jotter.BuildConfig
import com.openappslabs.jotter.ui.components.ClearAllDataDialog
import com.openappslabs.jotter.ui.components.DisableLockWarningDialog
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsGroup
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsItemArrow
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsItemDateFormat
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsItemEditView
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsItemGridView
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsItemNoteLock
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsItemSwitch
import com.openappslabs.jotter.ui.screens.settingsscreen.components.SettingsItemTimeFormat
import com.openappslabs.jotter.ui.screens.settingsscreen.components.TinyGap
import com.openappslabs.jotter.ui.theme.rememberJotterHaptics
import com.openappslabs.jotter.utils.BiometricAuthUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onManageTagsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit,
    onDataManagementClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: SettingsScreenViewModel = hiltViewModel()
) {
    val haptics = rememberJotterHaptics()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        )
        return
    }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDisableLockWarningDialog by remember { mutableStateOf(false) }

    val appBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    Surface(
                        onClick = {
                            haptics.click()
                            onBackClick()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = appBarColors
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = innerPadding.calculateTopPadding() + 8.dp, 
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsGroup(title = "Appearance") {
                    SettingsItemSwitch(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Reduce eye strain",
                        checked = uiState.isDarkMode,
                        onCheckedChange = viewModel::updateDarkMode
                    )
                    TinyGap()

                    SettingsItemSwitch(
                        icon = Icons.Default.Brightness2,
                        title = "True Black Mode",
                        subtitle = "Use pure black for OLED displays",
                        checked = uiState.isTrueBlackEnabled,
                        onCheckedChange = viewModel::updateTrueBlackMode
                    )
                    TinyGap()

                    SettingsItemSwitch(
                        icon = Icons.Default.ColorLens,
                        title = "Dynamic Colors",
                        subtitle = "Adapt to wallpaper",
                        checked = uiState.isDynamicColor,
                        onCheckedChange = viewModel::updateDynamicColor
                    )

                    TinyGap()
                    SettingsItemEditView(
                        icon = Icons.Default.Edit,
                        title = "Default Open Mode",
                        subtitle = "View OR Edit",
                        isEditDefault = uiState.defaultOpenInEdit,
                        onToggleEditDefault = viewModel::updateDefaultOpenInEdit
                    )

                    TinyGap()

                    SettingsItemGridView(
                        icon = Icons.Outlined.Dashboard,
                        title = "Default View Mode",
                        subtitle = if (uiState.isGridView) "Grid View" else "List View",
                        isGridView = uiState.isGridView,
                        onToggle = {
                            viewModel.updateGridView(!uiState.isGridView)
                        }
                    )

                    TinyGap()

                    SettingsItemTimeFormat(
                        icon = Icons.Outlined.Schedule,
                        title = "Default Time Format",
                        subtitle = if (uiState.is24HourFormat) "24 Hour Clock" else "12 Hour (AM/PM)",
                        is24Hour = uiState.is24HourFormat,
                        onToggle = viewModel::updateTimeFormat
                    )

                    TinyGap()

                    SettingsItemDateFormat(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Date Format",
                        subtitle = "Change how dates appear",
                        currentFormat = uiState.dateFormat,
                        onFormatSelected = viewModel::updateDateFormat
                    )
                }
            }

            item {
                SettingsGroup(title = "General & Navigation") {
                    SettingsItemArrow(
                        icon = Icons.AutoMirrored.Filled.Label,
                        title = "Manage Tags",
                        subtitle = "Add or remove note tags",
                        onClick = onManageTagsClick
                    )
                    TinyGap()

                    SettingsItemArrow(
                        icon = Icons.Default.Archive,
                        title = "Archived Notes",
                        subtitle = "Notes you archive appear here",
                        onClick = onArchiveClick
                    )
                    TinyGap()

                    SettingsItemArrow(
                        icon = Icons.Default.Delete,
                        title = "Trash",
                        subtitle = "Notes you delete appear here",
                        onClick = onTrashClick
                    )
                    TinyGap()

                    SettingsItemSwitch(
                        icon = Icons.Default.Add,
                        title = "Show Add Tag Button",
                        subtitle = "Show/Hide '+' on Home screen",
                        checked = uiState.showAddCategoryButton,
                        onCheckedChange = viewModel::updateShowAddCategoryButton
                    )
                    TinyGap()

                    SettingsItemSwitch(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on touch interactions",
                        checked = uiState.isHapticEnabled,
                        onCheckedChange = viewModel::updateHapticEnabled
                    )
                }
            }

            item {
                SettingsGroup(title = "Security") {
                    val authSupport = remember(context) {
                        BiometricAuthUtil.getAuthenticationSupport(context)
                    }
                    val isBiometricAvailable = authSupport.hasFingerprint || authSupport.hasDeviceCredential

                    AnimatedVisibility(
                        visible = isBiometricAvailable,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            SettingsItemNoteLock(
                                title = "Note Lock",
                                subtitle = "Require authentication to open",
                                checked = uiState.isBiometricEnabled,
                                authSupport = authSupport,
                                onCheckedChange = { isEnabled ->
                                    if (isEnabled) {
                                        viewModel.updateBiometricEnabled(true)
                                    } else {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricAuthUtil.authenticate(
                                                activity = activity,
                                                title = "Confirm Identity",
                                                subtitle = "Authenticate To Disable Note Lock",
                                                onSuccess = {
                                                    showDisableLockWarningDialog = true
                                                },
                                                onError = { }
                                            )
                                        }
                                    }
                                }
                            )
                            TinyGap()
                        }
                    }

//                    SettingsItemSwitch(
//                        icon = Icons.Default.VpnKey,
//                        title = "App Lock",
//                        subtitle = "Require authentication to open app",
//                        checked = uiState.isAppLockEnabled,
//                        onCheckedChange = { targetState ->
//                            val activity = context as? FragmentActivity
//                            if (activity != null) {
//                                val authSubtitle = if (targetState) {
//                                    "Authenticate To Enable App Lock"
//                                } else {
//                                    "Authenticate To Disable App Lock"
//                                }
//                                BiometricAuthUtil.authenticate(
//                                    activity = activity,
//                                    title = "Confirm Identity",
//                                    subtitle = authSubtitle,
//                                    onSuccess = {
//                                        viewModel.updateAppLockEnabled(targetState)
//                                    },
//                                    onError = { }
//                                )
//                            }
//                        }
//                    )
//
//                    TinyGap()

                    SettingsItemSwitch(
                        icon = Icons.Default.Security,
                        title = "Secure Screen",
                        subtitle = "Disable screenshots",
                        checked = uiState.isSecureMode,
                        onCheckedChange = viewModel::updateSecureMode
                    )
                }
            }

            item {
                SettingsGroup(title = "Data Management") {
                    SettingsItemArrow(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restore",
                        subtitle = "Export or import notes",
                        onClick = onDataManagementClick
                    )
//                    TinyGap()

//                    SettingsItemArrow(
//                        icon = Icons.Default.DeleteForever,
//                        title = "Clear All Local Data",
//                        subtitle = "Reset app and delete all notes permanently",
//                        onClick = { showClearAllDialog = true },
//                        isDestructive = true
//                    )
                }
            }

            item {
                SettingsGroup(title = "About") {
                    SettingsItemArrow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        onClick = onPrivacyPolicyClick
                    )

                    TinyGap()

                    SettingsItemArrow(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = BuildConfig.VERSION_NAME,
                        onClick = onAboutClick
                    )
                }
            }
        }

        if (showDisableLockWarningDialog) {
            DisableLockWarningDialog(
                onDismiss = { 
                    haptics.click()
                    showDisableLockWarningDialog = false 
                },
                onConfirm = {
                    haptics.heavy()
                    showDisableLockWarningDialog = false
                    viewModel.updateBiometricEnabled(false)
                }
            )
        }
    }
}