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

package com.openappslabs.jotter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.openappslabs.jotter.ui.theme.rememberJotterHaptics
import com.openappslabs.jotter.utils.buildAnnotated
import com.openappslabs.jotter.utils.decodeSpans
import com.openappslabs.jotter.utils.lineStartOffsets

@Composable
fun RichContentView(
    content: String,
    contentAnnotations: String,
    baseStyle: TextStyle,
    onToggleChecklist: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberJotterHaptics()
    val spans = remember(content, contentAnnotations) { decodeSpans(contentAnnotations) }
    val annotated = remember(content, spans) { buildAnnotated(content, spans) }
    val lines = remember(content) { content.split("\n") }
    val offsets = remember(content) { lineStartOffsets(content) }
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            val lineStart = offsets.getOrNull(index) ?: 0
            val lineEnd = (lineStart + line.length).coerceAtMost(annotated.length)
            val lineAnnotated = annotated.subSequence(lineStart, lineEnd)
            val trimmed = line.trimStart()

            when {
                trimmed.startsWith("- [ ] ") ||
                    trimmed.startsWith("- [x] ") ||
                    trimmed.startsWith("- [X] ") -> {
                    val checked = trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ")
                    val markerEnd = line.indexOf(trimmed) + 6
                    val body = lineAnnotated.subSequence(markerEnd, line.length)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.tick()
                                onToggleChecklist(index, !checked)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleChecklist(index, it) },
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = body,
                            style = baseStyle.copy(
                                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (checked) mutedColor else Color.Unspecified
                            )
                        )
                    }
                }

                trimmed.startsWith("- ") ||
                    trimmed.startsWith("* ") ||
                    trimmed.startsWith("+ ") -> {
                    val contentStart = line.indexOf(trimmed) + 2
                    val body = lineAnnotated.subSequence(contentStart, line.length)
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "• ",
                            style = baseStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = body, style = baseStyle)
                    }
                }

                else -> {
                    Text(
                        text = lineAnnotated,
                        style = baseStyle
                    )
                }
            }
        }
    }
}