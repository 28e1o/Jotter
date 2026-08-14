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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONArray
import org.json.JSONObject

enum class RichFont(val id: String, val label: String) {
    Default("", "Teks Biasa"),
    Serif("serif", "Serif"),
    Mono("mono", "Monospace"),
    Cursive("cursive", "Kursif");

    val fontFamily: FontFamily
        get() = when (this) {
            Default -> FontFamily.Default
            Serif -> FontFamily.Serif
            Mono -> FontFamily.Monospace
            Cursive -> FontFamily.Cursive
        }

    companion object {
        fun fromId(id: String?): RichFont {
            return entries.firstOrNull { it.id == id } ?: Default
        }
    }
}

data class TextSpan(
    val start: Int,
    val end: Int,
    val font: RichFont = RichFont.Default,
    val bold: Boolean = false,
    val italic: Boolean = false
)

data class ActiveFormat(
    val font: RichFont = RichFont.Default,
    val bold: Boolean = false,
    val italic: Boolean = false
)

fun buildAnnotated(text: String, spans: List<TextSpan>): AnnotatedString {
    val styleRanges = spans.map { span ->
        AnnotatedString.Range(
            SpanStyle(
                fontFamily = if (span.font != RichFont.Default) span.font.fontFamily else null,
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                color = Color.Unspecified
            ),
            span.start,
            span.end
        )
    }
    return AnnotatedString(text, spanStyles = styleRanges)
}

fun encodeSpans(spans: List<TextSpan>): String {
    if (spans.isEmpty()) return ""
    val array = JSONArray()
    spans.forEach { span ->
        array.put(
            JSONObject()
                .put("s", span.start)
                .put("e", span.end)
                .put("f", span.font.id)
                .put("b", span.bold)
                .put("i", span.italic)
        )
    }
    return array.toString()
}

fun decodeSpans(json: String?): List<TextSpan> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        val spans = mutableListOf<TextSpan>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            spans.add(
                TextSpan(
                    start = obj.optInt("s", 0),
                    end = obj.optInt("e", 0),
                    font = RichFont.fromId(obj.optString("f", "")),
                    bold = obj.optBoolean("b", false),
                    italic = obj.optBoolean("i", false)
                )
            )
        }
        normalizeSpans(spans)
    } catch (e: Exception) {
        emptyList()
    }
}

fun normalizeSpans(spans: List<TextSpan>): List<TextSpan> {
    val sorted = spans.filter { it.end > it.start }
        .sortedWith(compareBy({ it.start }, { it.end }))
    val out = mutableListOf<TextSpan>()
    sorted.forEach { span ->
        val last = out.lastOrNull()
        if (last != null && last.end >= span.start && last.sameAttrs(span)) {
            out[out.size - 1] = last.copy(end = maxOf(last.end, span.end))
        } else {
            out.add(span)
        }
    }
    return out
}

private fun TextSpan.sameAttrs(other: TextSpan): Boolean {
    return font == other.font && bold == other.bold && italic == other.italic
}

fun attributesAt(spans: List<TextSpan>, offset: Int): ActiveFormat {
    var font = RichFont.Default
    var bold = false
    var italic = false
    spans.filter { it.start <= offset && it.end > offset }.forEach { span ->
        if (span.font != RichFont.Default) font = span.font
        if (span.bold) bold = true
        if (span.italic) italic = true
    }
    return ActiveFormat(font, bold, italic)
}

fun applyFormat(
    spans: List<TextSpan>,
    start: Int,
    end: Int,
    format: ActiveFormat
): List<TextSpan> {
    val s = start.coerceAtLeast(0)
    val e = end.coerceAtLeast(s)
    if (e <= s) return spans
    val result = mutableListOf<TextSpan>()
    spans.forEach { span ->
        if (span.end <= s || span.start >= e) {
            result.add(span)
        } else {
            if (span.start < s) result.add(span.copy(end = s))
            if (span.end > e) result.add(span.copy(start = e))
        }
    }
    result.add(TextSpan(s, e, format.font, format.bold, format.italic))
    return normalizeSpans(result)
}

fun editSpans(
    spans: List<TextSpan>,
    start: Int,
    end: Int,
    replacementLength: Int
): List<TextSpan> {
    if (end <= start && replacementLength == 0) return spans
    val delta = replacementLength - (end - start)
    val out = mutableListOf<TextSpan>()
    spans.forEach { span ->
        when {
            span.end <= start -> out.add(span)
            span.start >= end -> out.add(span.copy(start = span.start + delta, end = span.end + delta))
            span.start >= start && span.end <= end -> {
                // fully replaced
            }
            span.start < start && span.end > end -> {
                val newEnd = span.end + delta
                if (newEnd > span.start) out.add(span.copy(end = newEnd))
            }
            span.start < start -> {
                out.add(span.copy(end = start))
                val rightStart = end + delta
                if (span.end > end && rightStart < span.end + delta) {
                    out.add(span.copy(start = rightStart, end = span.end + delta))
                }
            }
            else -> {
                val newStart = start + replacementLength
                val newEnd = span.end + delta
                if (newEnd > newStart) out.add(span.copy(start = newStart, end = newEnd))
            }
        }
    }
    return normalizeSpans(out)
}

fun lineStartOffsets(text: String): List<Int> {
    val offsets = mutableListOf(0)
    text.forEachIndexed { index, c ->
        if (c == '\n') offsets.add(index + 1)
    }
    return offsets
}

fun lineIndexForOffset(text: String, offset: Int): Int {
    var line = 0
    for (i in 0 until offset.coerceIn(0, text.length)) {
        if (text[i] == '\n') line++
    }
    return line
}