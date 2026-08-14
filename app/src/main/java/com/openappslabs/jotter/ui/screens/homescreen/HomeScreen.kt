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

package com.openappslabs.jotter.ui.screens.homescreen

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.openappslabs.jotter.ui.components.SortSheet
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openappslabs.jotter.ui.components.CategoryBar
import com.openappslabs.jotter.ui.components.CategoryItems
import com.openappslabs.jotter.ui.components.FAB
import com.openappslabs.jotter.ui.components.NoteCard
import com.openappslabs.jotter.ui.components.QuickActionSheet
import com.openappslabs.jotter.ui.components.SearchBar
import com.openappslabs.jotter.ui.theme.rememberJotterHaptics
import com.openappslabs.jotter.utils.BiometricAuthUtil
import com.openappslabs.jotter.utils.NoteTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: (String?) -> Unit,
    onTemplateNoteClick: (NoteTemplate) -> Unit,
    onAddCategoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val haptics = rememberJotterHaptics()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyStaggeredGridState()
    val context = LocalContext.current
    val locale = Locale.getDefault()

    var showSortSheet by remember { mutableStateOf(false) }
    var showQuickActionSheet by remember { mutableStateOf(false) }
    val sortSheetState = rememberModalBottomSheetState()
    val dateFormatter = remember(uiState.dateFormat, uiState.isGridView, locale) {
        val format = if (!uiState.isGridView) {
            if (uiState.dateFormat.contains("/")) "${uiState.dateFormat}/yyyy"
            else "${uiState.dateFormat} yyyy"
        } else {
            uiState.dateFormat
        }
        SimpleDateFormat(format, locale)
    }

    LaunchedEffect(uiState.selectedCategory) {
        listState.animateScrollToItem(0)
    }

    LaunchedEffect(uiState.sortType, uiState.sortDirection) {
        listState.animateScrollToItem(0)
    }

    Scaffold(
        floatingActionButton = {
            FAB(
                onClick = {
                    val category = if (uiState.selectedCategory == "All") null else uiState.selectedCategory
                    onAddNoteClick(category)
                },
                onLongClick = {
                    showQuickActionSheet = true
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.isHomeFocusMode) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onProfileClick = {  },
                    onSettingsClick = {
                        haptics.click()
                        onSettingsClick()
                    },
                    modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp)
                )

                CategoryBar(
                    categories = CategoryItems(uiState.allAvailableCategories),
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = { viewModel.selectCategory(it) },
                    onAddCategoryClick = onAddCategoryClick,
                    showAddButton = uiState.showAddCategoryButton,
                    showSortButton = uiState.showSortButton,
                    modifier = Modifier.padding(bottom = 8.dp),
                    sortDirection = uiState.sortDirection,
                    sortType = uiState.sortType,
                    categoryColors = uiState.categoryColors,
                    onSortClick = {
                        haptics.tick()
                        showSortSheet = true
                    }
                )
            }

            LazyVerticalStaggeredGrid(
                state = listState,
                columns = StaggeredGridCells.Fixed(if (uiState.isGridView) 2 else 1),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp
            ) {
                items(uiState.allNotes, key = { it.id }) { note ->
                    val dateStr = remember(note.createdTime, uiState.dateFormat, uiState.isGridView, locale) {
                        dateFormatter.format(Date(note.createdTime))
                    }
                    NoteCard(
                        note = note,
                        date = dateStr,
                        isGridView = uiState.isGridView,
                        categoryColor = uiState.categoryColors[note.category],
                        onClick   = { 
                            haptics.tick()
                            viewModel.onNoteClicked(note.id)
                            if (note.isLocked && uiState.isBiometricEnabled) {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    BiometricAuthUtil.authenticate(
                                        activity = activity,
                                        title = "Buka Kunci Catatan",
                                        subtitle = "Autentikasi untuk melihat catatan terkunci ini",
                                        onSuccess = {
                                            onNoteClick(note.id)
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Autentikasi gagal", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    onNoteClick(note.id)
                                }
                            } else {
                                onNoteClick(note.id)
                            }
                        },
                        modifier  = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SortSheet(
            sheetState = sortSheetState,
            currentSortType = uiState.sortType,
            currentSortDirection = uiState.sortDirection,
            onSortTypeChange = {
                viewModel.setSortType(it)
            },
            onSortDirectionChange = {
                viewModel.setSortDirection(it)
            },
            onDismissRequest = { showSortSheet = false }
        )
    }

    if (showQuickActionSheet) {
        QuickActionSheet(
            categories = uiState.allAvailableCategories,
            selectedCategory = uiState.selectedCategory,
            onTemplateSelected = { template ->
                showQuickActionSheet = false
                onTemplateNoteClick(template)
            },
            onCategorySelected = { category ->
                showQuickActionSheet = false
                viewModel.selectCategory(category)
            },
            onDismiss = { showQuickActionSheet = false }
        )
    }
}