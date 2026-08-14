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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.openappslabs.jotter.utils.MarkdownBlock
import com.openappslabs.jotter.utils.parseMarkdownBlocks

@Composable
fun MarkdownContent(
    content: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    onToggleChecklist: (Int, Boolean) -> Unit = { _, _ -> }
) {
    val uriHandler = LocalUriHandler.current
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val linkColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    val blocks = remember(content, baseStyle, codeBackground, linkColor) {
        parseMarkdownBlocks(content, baseStyle, codeBackground, linkColor) { url ->
            uriHandler.openUri(url)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> baseStyle.copy(fontSize = baseStyle.fontSize * 1.45f, fontWeight = FontWeight.Bold)
                        2 -> baseStyle.copy(fontSize = baseStyle.fontSize * 1.25f, fontWeight = FontWeight.Bold)
                        else -> baseStyle.copy(fontSize = baseStyle.fontSize * 1.12f, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = block.text, style = style)
                }

                is MarkdownBlock.Paragraph -> Text(text = block.text, style = baseStyle)

                is MarkdownBlock.ListItem -> Row(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "${block.bullet} ",
                        style = baseStyle.copy(fontWeight = FontWeight.Bold),
                        color = primary
                    )
                    Text(text = block.text, style = baseStyle)
                }

                is MarkdownBlock.ChecklistItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleChecklist(block.line, !block.checked) },
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (block.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (block.checked) "Selesai" else "Tandai selesai",
                            tint = if (block.checked) primary else onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = block.text,
                            style = if (block.checked) {
                                baseStyle.copy(
                                    color = onSurfaceVariant.copy(alpha = 0.6f),
                                    textDecoration = TextDecoration.LineThrough
                                )
                            } else {
                                baseStyle
                            }
                        )
                    }
                }

                is MarkdownBlock.Blockquote -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .background(onSurfaceVariant.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = block.text, style = baseStyle.copy(color = onSurfaceVariant))
                }

                is MarkdownBlock.Divider -> HorizontalDivider(color = onSurfaceVariant.copy(alpha = 0.2f))
            }
        }
    }
}
