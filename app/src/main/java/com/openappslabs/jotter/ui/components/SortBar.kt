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

package com.openappslabs.jotter.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.openappslabs.jotter.ui.theme.rememberJotterHaptics

enum class SortDirection {
    ASCENDING, DESCENDING
}

enum class SortType {
    ALPHABETICAL, CREATED, LAST_UPDATED;

    fun next(): SortType {
        val values = entries.toTypedArray()
        val nextOrdinal = (ordinal + 1) % values.size
        return values[nextOrdinal]
    }
}

@Composable
fun SortBar(
    modifier: Modifier = Modifier,
    sortDirection: SortDirection,
    sortType: SortType,
    onSortDirectionClick: () -> Unit,
    onSortTypeClick: () -> Unit
) {
    val haptics = rememberJotterHaptics()

    Surface(
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 32.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .clickable {
                        haptics.tick()
                        onSortDirectionClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                val rotationAngle by animateFloatAsState(
                    targetValue = if (sortDirection == SortDirection.DESCENDING) 180f else 0f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "SortRotation"
                )

                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            rotationZ = rotationAngle
                        },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            VerticalDivider(
                modifier = Modifier.height(16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 32.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .clickable {
                        haptics.tick()
                        onSortTypeClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.Crossfade(
                    targetState = sortType,
                    animationSpec = tween(300),
                    label = "SortIconFade"
                ) { targetType ->
                    val icon = when (targetType) {
                        SortType.ALPHABETICAL -> Icons.Default.SortByAlpha
                        SortType.CREATED -> Icons.Default.AccessTime
                        SortType.LAST_UPDATED -> Icons.Default.Update
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}