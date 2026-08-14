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

package com.openappslabs.jotter.ui.screens.notedetailscreen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openappslabs.jotter.ui.components.CategoryItems
import com.openappslabs.jotter.ui.components.CategorySheet
import com.openappslabs.jotter.ui.components.DeleteNoteDialog
import com.openappslabs.jotter.ui.components.DiscardChangesDialog
import com.openappslabs.jotter.ui.components.EditViewButton
import com.openappslabs.jotter.ui.components.MarkdownContent
import com.openappslabs.jotter.ui.components.NoteActionSheet
import com.openappslabs.jotter.ui.components.RestoreNoteDialog
import com.openappslabs.jotter.ui.components.RichContentView
import com.openappslabs.jotter.ui.theme.rememberJotterHaptics
import com.openappslabs.jotter.utils.ActiveFormat
import com.openappslabs.jotter.utils.NoteUtils
import com.openappslabs.jotter.utils.RichFont
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onManageCategoryClick: () -> Unit = {},
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val haptics = rememberJotterHaptics()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val contentEditor by viewModel.contentEditor.collectAsStateWithLifecycle()
    val activeFormat by viewModel.activeFormat.collectAsStateWithLifecycle()
    val sessionElapsed by viewModel.sessionElapsed.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showRestoreNoteDialog by remember { mutableStateOf(false) }
    var pendingDiscard by remember { mutableStateOf(false) }
    var showNoteActionSheet by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) }
    val noteActionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val contentFocusRequester = remember { FocusRequester() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let {
                val textToSave = NoteUtils.formatNote(uiState.title, uiState.content)
                val success = NoteUtils.saveTextToUri(context, it, textToSave)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (success) "Catatan berhasil diekspor" else "Gagal mengekspor catatan"
                    )
                }
            }
        }
    )

    var isViewMode by remember(uiState.isNotePersisted, userPrefs.defaultOpenInEdit) {
        val initialViewMode = if (uiState.isNotePersisted) {
            !userPrefs.defaultOpenInEdit
        } else {
            false
        }
        mutableStateOf(initialViewMode)
    }

    val isSaveEnabled = !isViewMode && uiState.isModified && (uiState.title.isNotBlank() || uiState.content.isNotBlank())

    val locale = Locale.getDefault()
    val dateString = remember(uiState.createdTime, userPrefs.is24HourFormat, userPrefs.dateFormat, locale) {
        val datePattern = if (userPrefs.dateFormat.contains("/")) "${userPrefs.dateFormat}/yyyy"
        else "${userPrefs.dateFormat} yyyy"
        val timePattern = if (userPrefs.is24HourFormat) "HH:mm" else "hh:mm a"
        val pattern = "$datePattern, $timePattern"
        SimpleDateFormat(pattern, locale).format(Date(uiState.createdTime))
    }

    val modifiedDateString = remember(uiState.lastEdited, userPrefs.is24HourFormat, userPrefs.dateFormat, locale) {
        val datePattern = if (userPrefs.dateFormat.contains("/")) "${userPrefs.dateFormat}/yyyy"
        else "${userPrefs.dateFormat} yyyy"
        val timePattern = if (userPrefs.is24HourFormat) "HH:mm" else "hh:mm a"
        val pattern = "$datePattern, $timePattern"
        SimpleDateFormat(pattern, locale).format(Date(uiState.lastEdited))
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    val titleStyle = remember(onSurfaceColor, userPrefs.fontSizeScale) {
        val size = (30f * userPrefs.fontSizeScale).coerceAtLeast(14f)
        TextStyle(
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor
        )
    }

    val contentStyle = remember(onSurfaceColor, userPrefs.fontSizeScale, userPrefs.lineSpacingScale) {
        val size = (18f * userPrefs.fontSizeScale).coerceAtLeast(12f)
        val lineHeight = (28f * userPrefs.lineSpacingScale).coerceAtLeast(18f)
        TextStyle(
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = FontWeight.Normal,
            color = onSurfaceColor.copy(alpha = 0.85f)
        )
    }

    val cursorBrush = remember(primaryColor) {
        SolidColor(primaryColor)
    }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && pendingDiscard) {
            pendingDiscard = false
            showDiscardDialog = true
        }
    }

    LaunchedEffect(isViewMode) {
        if (isViewMode) {
            isFocusMode = false
        }
    }

    fun handleBack() {
        if (!isViewMode && uiState.isModified) {
            if (isImeVisible) {
                pendingDiscard = true
                keyboardController?.hide()
            } else {
                showDiscardDialog = true
            }
        } else {
            onBackClick()
        }
    }

    val shouldInterceptBack = !isViewMode && uiState.isModified
    BackHandler(enabled = shouldInterceptBack) {
        handleBack()
    }

    val isArchivedOrTrashed = uiState.isArchived || uiState.isTrashed

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!isFocusMode || isViewMode) {
                CenterAlignedTopAppBar(
                    title = {
                    if (uiState.isNotePersisted && !isArchivedOrTrashed) {
                        EditViewButton(
                            isEditing = !isViewMode,
                            onToggle = {
                                haptics.tick()
                                isViewMode = !isViewMode
                            },
                            iconButtonSize = 48.dp
                        )
                    } else {
                        Text(
                            text = "",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = {
                            haptics.click()
                            handleBack()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val showCloseIcon = !isViewMode && uiState.isModified
                            Icon(
                                imageVector = if (showCloseIcon) Icons.Default.Close else Icons.Default.ChevronLeft,
                                contentDescription = if (showCloseIcon) "Tutup" else "Kembali",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isArchivedOrTrashed) {
                        Surface(
                            onClick = {
                                haptics.click()
                                showRestoreNoteDialog = true
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            enabled = true,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "Pulihkan/Batalkan Arsip",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else if (isViewMode) {
                        Surface(
                            onClick = {
                                haptics.click()
                                showNoteActionSheet = true
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Tindakan",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.canUndo) {
                                Surface(
                                    onClick = {
                                        haptics.tick()
                                        viewModel.undo()
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                                            contentDescription = "Urungkan",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                onClick = {
                                    haptics.tick()
                                    isFocusMode = true
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = "Mode fokus mengetik",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    haptics.success()
                                    viewModel.saveNote()
                                    isViewMode = true
                                    keyboardController?.hide()
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                enabled = isSaveEnabled,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Simpan",
                                        tint = if (isSaveEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isArchivedOrTrashed && !isViewMode) {
                            contentFocusRequester.requestFocus()
                        }
                    }
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    readOnly = isViewMode || isArchivedOrTrashed,
                    textStyle = titleStyle,
                    cursorBrush = cursorBrush,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contentFocusRequester),
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.title.isEmpty() && !isViewMode && !isArchivedOrTrashed) {
                                Text(
                                    text = "Tanpa Judul",
                                    style = titleStyle.copy(color = onSurfaceColor.copy(alpha = 0.3f))
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!isViewMode && !isArchivedOrTrashed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = NoteUtils.formatDuration(sessionElapsed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (isViewMode || isArchivedOrTrashed) {
                    if (uiState.contentAnnotations.isBlank()) {
                        MarkdownContent(
                            content = uiState.content,
                            baseStyle = contentStyle,
                            onToggleChecklist = { line, checked ->
                                if (!isArchivedOrTrashed) {
                                    viewModel.toggleChecklist(line, checked)
                                }
                            }
                        )
                    } else {
                        RichContentView(
                            content = uiState.content,
                            contentAnnotations = uiState.contentAnnotations,
                            baseStyle = contentStyle,
                            onToggleChecklist = { line, checked ->
                                if (!isArchivedOrTrashed) {
                                    viewModel.toggleChecklist(line, checked)
                                }
                            }
                        )
                    }
                } else {
                    FormattingToolbar(
                        activeFormat = activeFormat,
                        onFontSelected = viewModel::setEditorFont,
                        onToggleBold = viewModel::toggleBold,
                        onToggleItalic = viewModel::toggleItalic,
                        onToggleList = viewModel::toggleBulletList,
                        onToggleNumberedList = viewModel::toggleNumberedList,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    BasicTextField(
                        value = contentEditor,
                        onValueChange = { viewModel.updateContent(it) },
                        readOnly = false,
                        textStyle = contentStyle,
                        cursorBrush = cursorBrush,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(contentFocusRequester),
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.content.isEmpty()) {
                                    Text(
                                        text = "Mulai mengetik...",
                                        style = contentStyle.copy(color = onSurfaceColor.copy(alpha = 0.3f))
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isFocusMode && !isViewMode) {
                Surface(
                    onClick = {
                        haptics.click()
                        isFocusMode = false
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.FullscreenExit,
                            contentDescription = "Keluar mode fokus",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        CategorySheet(
            categories = CategoryItems(availableCategories),
            selectedCategory = uiState.category,
            onCategorySelect = { newCategory ->
                haptics.tick()
                viewModel.updateCategory(newCategory)
                isViewMode = false
            },
            onManageCategoriesClick = {
                haptics.click()
                onManageCategoryClick()
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDismiss = {
                haptics.click()
                showDiscardDialog = false
            },
            onConfirm = {
                haptics.tick()
                showDiscardDialog = false
                if (uiState.isNotePersisted) {
                    viewModel.undoChanges()
                    isViewMode = true
                } else {
                    onBackClick()
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteNoteDialog(
            onDismiss = {
                haptics.click()
                showDeleteDialog = false
            },
            onConfirm = {
                haptics.heavy()
                showDiscardDialog = false
                viewModel.deleteNote()
                onBackClick()
            }
        )
    }

    if (showNoteActionSheet) {
        NoteActionSheet(
            sheetState = noteActionSheetState,
            scope = scope,
            createdDate = dateString,
            modifiedDate = modifiedDateString,
            wordCount = NoteUtils.countWords(uiState.content),
            charCount = NoteUtils.countCharacters(uiState.content),
            category = uiState.category,
            isPinned = uiState.isPinned,
            isLocked = uiState.isLocked,
            onDeleteClick = {
                haptics.heavy()
                showNoteActionSheet = false
                viewModel.deleteNote()
                onBackClick()
            },
            onArchiveClick = {
                haptics.tick()
                showNoteActionSheet = false
                viewModel.archiveNote()
                onBackClick()
            },
            onShareClick = {
                haptics.click()
                val textToShare = NoteUtils.formatNote(uiState.title, uiState.content)
                NoteUtils.shareNote(context, textToShare)
                showNoteActionSheet = false
            },
            onDuplicateClick = {
                haptics.click()
                viewModel.duplicateNote { newId ->
                    showNoteActionSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Catatan diduplikasi")
                    }
                }
            },
            onExportClick = {
                haptics.click()
                showNoteActionSheet = false
                val fileName = if (uiState.title.isNotBlank()) "${uiState.title}.txt" else "JotterNote.txt"
                exportLauncher.launch(fileName)
            },
            onCopyClick = {
                haptics.success()
                val textToCopy = NoteUtils.formatNote(uiState.title, uiState.content)
                NoteUtils.copyToClipboard(context, textToCopy)
                showNoteActionSheet = false
            },
            onPinClick = {
                haptics.tick()
                viewModel.togglePin()
            },
            onLockClick = {
                haptics.tick()
                if (userPrefs.isBiometricEnabled) {
                    viewModel.toggleLock()
                } else {
                    scope.launch {
                        if (snackbarHostState.currentSnackbarData == null) {
                            snackbarHostState.showSnackbar(
                                message = "Aktifkan Kunci Catatan di Pengaturan untuk menggunakan fitur ini",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            },
            onCategoryClick = {
                haptics.click()
                showCategorySheet = true
            },
            onDismissRequest = { showNoteActionSheet = false }
        )
    }

    if (showRestoreNoteDialog) {
        RestoreNoteDialog(
            onDismiss = {
                haptics.click()
                showRestoreNoteDialog = false
            },
            onConfirm = {
                haptics.success()
                showRestoreNoteDialog = false
                viewModel.restoreNote()
                onBackClick()
            }
        )
    }
}

@Composable
private fun FormattingToolbar(
    activeFormat: ActiveFormat,
    onFontSelected: (RichFont) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleList: () -> Unit,
    onToggleNumberedList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFontMenu by remember { mutableStateOf(false) }
    val haptics = rememberJotterHaptics()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                onClick = {
                    haptics.tick()
                    showFontMenu = true
                },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = activeFormat.font.fontFamily
                    )
                }
            }
            DropdownMenu(
                expanded = showFontMenu,
                onDismissRequest = { showFontMenu = false }
            ) {
                RichFont.entries.forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = font.label,
                                fontFamily = font.fontFamily
                            )
                        },
                        onClick = {
                            showFontMenu = false
                            haptics.tick()
                            onFontSelected(font)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FormatToolbarButton(
            active = activeFormat.bold,
            icon = Icons.Default.FormatBold,
            contentDescription = "Tebal",
            onClick = {
                haptics.tick()
                onToggleBold()
            }
        )

        FormatToolbarButton(
            active = activeFormat.italic,
            icon = Icons.Default.FormatItalic,
            contentDescription = "Miring",
            onClick = {
                haptics.tick()
                onToggleItalic()
            }
        )

        FormatToolbarButton(
            active = false,
            icon = Icons.Default.FormatListBulleted,
            contentDescription = "Daftar Bulat",
            onClick = {
                haptics.tick()
                onToggleList()
            }
        )

        FormatToolbarButton(
            active = false,
            icon = Icons.Default.FormatListNumbered,
            contentDescription = "Daftar Angka",
            onClick = {
                haptics.tick()
                onToggleNumberedList()
            }
        )
    }
}

@Composable
private fun FormatToolbarButton(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}