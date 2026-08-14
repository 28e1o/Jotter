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

package com.openappslabs.jotter.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

sealed class MarkdownBlock {
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock()
    data class Heading(val text: AnnotatedString, val level: Int) : MarkdownBlock()
    data class ListItem(val text: AnnotatedString, val bullet: String) : MarkdownBlock()
    data class ChecklistItem(val text: AnnotatedString, val checked: Boolean, val line: Int) : MarkdownBlock()
    data class Blockquote(val text: AnnotatedString) : MarkdownBlock()
    data object Divider : MarkdownBlock()
}

fun parseMarkdownBlocks(
    text: String,
    baseStyle: TextStyle,
    codeBackground: Color,
    linkColor: Color,
    onLinkClick: (String) -> Unit
): List<MarkdownBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isNotEmpty()) {
            blocks.add(
                MarkdownBlock.Paragraph(
                    buildInline(paragraphLines.joinToString("\n"), baseStyle, codeBackground, linkColor, onLinkClick)
                )
            )
            paragraphLines.clear()
        }
    }

    for ((index, rawLine) in lines.withIndex()) {
        val line = rawLine
        when {
            line.isBlank() -> flushParagraph()

            isDivider(line) -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Divider)
            }

            else -> {
                val headingLevel = headingLevelOf(line)
                if (headingLevel > 0) {
                    flushParagraph()
                    blocks.add(
                        MarkdownBlock.Heading(
                            buildInline(line.substring(headingLevel + 1), baseStyle, codeBackground, linkColor, onLinkClick),
                            headingLevel
                        )
                    )
                    continue
                }

                val checklist = checklistOf(line)
                if (checklist != null) {
                    flushParagraph()
                    blocks.add(
                        MarkdownBlock.ChecklistItem(
                            buildInline(checklist.second, baseStyle, codeBackground, linkColor, onLinkClick),
                            checked = checklist.first,
                            line = index
                        )
                    )
                    continue
                }

                val listBullet = listBulletOf(line)
                if (listBullet != null) {
                    flushParagraph()
                    blocks.add(
                        MarkdownBlock.ListItem(
                            buildInline(listBullet.second, baseStyle, codeBackground, linkColor, onLinkClick),
                            bullet = listBullet.first
                        )
                    )
                    continue
                }

                if (line.startsWith("> ")) {
                    flushParagraph()
                    blocks.add(
                        MarkdownBlock.Blockquote(
                            buildInline(line.substring(2), baseStyle, codeBackground, linkColor, onLinkClick)
                        )
                    )
                    continue
                }

                paragraphLines.add(line)
            }
        }
    }
    flushParagraph()
    return blocks
}

private fun isDivider(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.length < 3) return false
    return trimmed.all { it == '-' || it == '*' || it == '_' } && trimmed.count { it == trimmed[0] } >= 3
}

private fun headingLevelOf(line: String): Int {
    for (level in 1..3) {
        val prefix = "#".repeat(level)
        if (line.startsWith("$prefix ")) return level
    }
    return 0
}

private fun checklistOf(line: String): Pair<Boolean, String>? {
    return when {
        line.startsWith("- [ ] ") -> false to line.substring(6)
        line.startsWith("- [x] ") || line.startsWith("- [X] ") -> true to line.substring(6)
        else -> null
    }
}

private fun listBulletOf(line: String): Pair<String, String>? {
    val trimmed = line.trimStart()
    return when {
        trimmed.startsWith("- ") -> "•" to trimmed.substring(2)
        trimmed.startsWith("* ") -> "•" to trimmed.substring(2)
        trimmed.startsWith("+ ") -> "•" to trimmed.substring(2)
        trimmed.matches(Regex("\\d+\\. .*")) -> {
            val dot = trimmed.indexOf('.')
            "${trimmed.substring(0, dot)}." to trimmed.substring(dot + 1).trimStart()
        }
        else -> null
    }
}

private fun buildInline(
    text: String,
    baseStyle: TextStyle,
    codeBackground: Color,
    linkColor: Color,
    onLinkClick: (String) -> Unit
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val n = text.length
    var i = 0
    var plainStart = 0
    var bold = false
    var italic = false
    var strike = false
    var code = false
    var linkUrl: String? = null

    fun appendRange(start: Int, end: Int, isCode: Boolean, isBold: Boolean, isItalic: Boolean, isStrike: Boolean, url: String?) {
        if (start >= end) return
        val segment = text.substring(start, end)
        val spanStyle = if (isCode) {
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = codeBackground.copy(alpha = 0.6f),
                fontSize = baseStyle.fontSize * 0.92f
            )
        } else {
            SpanStyle(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (isStrike) TextDecoration.LineThrough else TextDecoration.None,
                color = baseStyle.color
            )
        }

        if (url != null) {
            val linkStyle = spanStyle.copy(color = linkColor)
            builder.append(segment)
            builder.addLink(
                LinkAnnotation.Clickable(
                    tag = url,
                    styles = TextLinkStyles(style = linkStyle, hoveredStyle = linkStyle, pressedStyle = linkStyle),
                    linkInteractionListener = { annotation -> onLinkClick((annotation as LinkAnnotation.Clickable).tag) }
                ),
                builder.length - segment.length,
                builder.length
            )
        } else {
            builder.pushStyle(spanStyle)
            builder.append(segment)
            builder.pop()
        }
    }

    while (i < n) {
        val ch = text[i]
        when {
            code -> {
                if (ch == '`') {
                    appendRange(plainStart, i, isCode = true, isBold = false, isItalic = false, isStrike = false, url = null)
                    code = false
                    i++
                    plainStart = i
                } else {
                    i++
                }
            }

            ch == '`' -> {
                appendRange(plainStart, i, isCode = true, isBold = false, isItalic = false, isStrike = false, url = null)
                code = true
                i++
                plainStart = i
            }

            ch == '\\' && i + 1 < n -> {
                i += 2
            }

            i + 2 < n && text.startsWith("***", i) -> {
                appendRange(plainStart, i, isCode = false, isBold = bold, isItalic = italic, isStrike = strike, url = linkUrl)
                bold = !bold
                italic = !italic
                i += 3
                plainStart = i
            }

            i + 1 < n && text.startsWith("**", i) -> {
                appendRange(plainStart, i, isCode = false, isBold = bold, isItalic = italic, isStrike = strike, url = linkUrl)
                bold = !bold
                i += 2
                plainStart = i
            }

            i + 1 < n && text.startsWith("~~", i) -> {
                appendRange(plainStart, i, isCode = false, isBold = bold, isItalic = italic, isStrike = strike, url = linkUrl)
                strike = !strike
                i += 2
                plainStart = i
            }

            ch == '*' -> {
                appendRange(plainStart, i, isCode = false, isBold = bold, isItalic = italic, isStrike = strike, url = linkUrl)
                italic = !italic
                i++
                plainStart = i
            }

            ch == '[' && linkUrl == null -> {
                val close = text.indexOf(']', i + 1)
                if (close != -1 && close + 1 < n && text[close + 1] == '(') {
                    val closeParen = text.indexOf(')', close + 2)
                    if (closeParen != -1 && !text.substring(close + 2, closeParen).contains('\n')) {
                        appendRange(plainStart, i, isCode = false, isBold = bold, isItalic = italic, isStrike = strike, url = null)
                        linkUrl = text.substring(close + 2, closeParen)
                        i++
                        plainStart = i
                        continue
                    }
                }
                i++
            }

            ch == ']' && linkUrl != null && i + 1 < n && text[i + 1] == '(' -> {
                appendRange(plainStart, i, isCode = false, isBold = bold, isItalic = italic, isStrike = strike, url = linkUrl)
                val closeParen = text.indexOf(')', i + 2)
                linkUrl = null
                i = if (closeParen == -1) i + 1 else closeParen + 1
                plainStart = i
            }

            else -> i++
        }
    }
    appendRange(plainStart, n, isCode = code, isBold = bold, isItalic = italic, isStrike = strike, url = linkUrl)
    return builder.toAnnotatedString()
}
