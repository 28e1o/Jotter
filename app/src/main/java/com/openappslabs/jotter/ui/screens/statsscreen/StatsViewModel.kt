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

package com.openappslabs.jotter.ui.screens.statsscreen

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openappslabs.jotter.data.repository.NotesRepository
import com.openappslabs.jotter.utils.NoteUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {

    enum class Period(val days: Int, val label: String) {
        DAY(1, "1 Hari"),
        WEEK(7, "7 Hari"),
        MONTH(30, "1 Bulan"),
        YEAR(365, "1 Tahun")
    }

    @Immutable
    data class Bucket(val label: String, val words: Int)

    @Immutable
    data class StatsUiState(
        val period: Period = Period.WEEK,
        val totalWords: Int = 0,
        val totalNotes: Int = 0,
        val avgWordsPerDay: Int = 0,
        val streakDays: Int = 0,
        val buckets: List<Bucket> = emptyList()
    )

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setPeriod(period: Period) {
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val period = _uiState.value.period
            val notes = notesRepository.getAllNotesSync().filter { !it.isTrashed }
            val zone = ZoneId.systemDefault()
            val now = LocalDate.now()

            var totalWords = 0
            val wordsByDay = HashMap<LocalDate, Int>()
            val days = mutableSetOf<LocalDate>()

            notes.forEach { note ->
                val words = NoteUtils.countWords(note.title) + NoteUtils.countWords(note.content)
                totalWords += words
                val day = LocalDate.ofInstant(Instant.ofEpochMilli(note.createdTime), zone)
                wordsByDay[day] = (wordsByDay[day] ?: 0) + words
                days.add(day)
                days.add(LocalDate.ofInstant(Instant.ofEpochMilli(note.updatedTime), zone))
            }

            val buckets = when (period) {
                Period.DAY -> {
                    val todayStart = now.atStartOfDay(zone).toInstant().toEpochMilli()
                    val todayEnd = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val hourly = IntArray(24)
                    notes.forEach { note ->
                        val created = note.createdTime
                        if (created >= todayStart && created < todayEnd) {
                            val words = NoteUtils.countWords(note.title) + NoteUtils.countWords(note.content)
                            val hour = Instant.ofEpochMilli(created).atZone(zone).hour
                            hourly[hour] += words
                        }
                    }
                    hourly.mapIndexed { hour, words ->
                        Bucket(hour.toString().padStart(2, '0'), words)
                    }
                }
                Period.WEEK -> (6 downTo 0).map { offset ->
                    val day = now.minusDays(offset.toLong())
                    Bucket(
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        wordsByDay[day] ?: 0
                    )
                }
                Period.MONTH -> (29 downTo 0).map { offset ->
                    val day = now.minusDays(offset.toLong())
                    Bucket(day.dayOfMonth.toString(), wordsByDay[day] ?: 0)
                }
                Period.YEAR -> {
                    val months = ArrayList<Bucket>()
                    for (offset in 11 downTo 0) {
                        val monthStart = now.minusMonths(offset.toLong()).withDayOfMonth(1)
                        val words = (0 until monthStart.lengthOfMonth()).sumOf { dayOffset ->
                            wordsByDay[monthStart.plusDays(dayOffset.toLong())] ?: 0
                        }
                        months.add(
                            Bucket(
                                monthStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                words
                            )
                        )
                    }
                    months
                }
            }

            val streak = currentStreak(days)
            val avg = totalWords / period.days.coerceAtLeast(1)

            _uiState.value = StatsUiState(
                period = period,
                totalWords = totalWords,
                totalNotes = notes.size,
                avgWordsPerDay = avg,
                streakDays = streak,
                buckets = buckets
            )
        }
    }

    private fun currentStreak(days: Set<LocalDate>): Int {
        var streak = 0
        var cursor = LocalDate.now()
        if (cursor !in days) cursor = cursor.minusDays(1)
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}