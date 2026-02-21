/*
 * Copyright (c) 2026 Open Apps Labs
 *
 * This file is part of Jotter *
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

package com.openappslabs.jotter.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

enum class SortDirection {
    ASCENDING, DESCENDING
}

enum class SortType {
    ALPHABETICAL, CREATED, LAST_UPDATED
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SortBar() {
    var sortDirection by remember { mutableStateOf(SortDirection.ASCENDING) }
    var sortType by remember { mutableStateOf(SortType.ALPHABETICAL) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = {
                    sortDirection = if (sortDirection == SortDirection.ASCENDING) {
                        SortDirection.DESCENDING
                    } else {
                        SortDirection.ASCENDING
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Sort Direction",
                    modifier = if (sortDirection == SortDirection.DESCENDING) {
                        Modifier.graphicsLayer(
                            rotationZ = 180f
                        )
                    } else {
                        Modifier
                    }
                )
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                onClick = {
                    sortType = when (sortType) {
                        SortType.ALPHABETICAL -> SortType.CREATED
                        SortType.CREATED -> SortType.LAST_UPDATED
                        SortType.LAST_UPDATED -> SortType.ALPHABETICAL
                    }
                }
            ) {
                val icon = when (sortType) {
                    SortType.ALPHABETICAL -> Icons.AutoMirrored.Rounded.Sort
                    SortType.CREATED -> Icons.Default.AccessTime
                    SortType.LAST_UPDATED -> Icons.Default.Update
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "Sort Type"
                )
            }
        }
    )
}