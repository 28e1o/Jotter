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
import com.openappslabs.jotter.data.model.Note
import com.openappslabs.jotter.data.repository.NotesRepository
import com.openappslabs.jotter.data.repository.UserPreferences
import com.openappslabs.jotter.data.repository.UserPreferencesRepository
import com.openappslabs.jotter.utils.NoteUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
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
    data class CategorySlice(val label: String, val count: Int)

    @Immutable
    data class StatsUiState(
        val period: Period = Period.WEEK,
        val totalWords: Int = 0,
        val totalCharacters: Int = 0,
        val totalNotes: Int = 0,
        val avgWordsPerDay: Int = 0,
        val streakDays: Int = 0,
        val totalTimeMs: Long = 0,
        val longestNoteTitle: String = "",
        val longestNoteTimeMs: Long = 0,
        val buckets: List<Bucket> = emptyList(),
        val categorySlices: List<CategorySlice> = emptyList()
    )

    private val _period = MutableStateFlow(Period.WEEK)

    val uiState: StateFlow<StatsUiState> = combine(
        notesRepository.getAllNotesIncludingArchived(),
        userPreferencesRepository.userPreferencesFlow,
        _period
    ) { notes, prefs, period ->
        runCatching { computeStats(notes, prefs, period) }
            .getOrElse { StatsUiState(period = period) }
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StatsUiState()
        )

    fun setPeriod(period: Period) {
        _period.value = period
    }

    private fun computeStats(
        notes: List<Note>,
        prefs: UserPreferences,
        period: Period
    ): StatsUiState {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()

        var totalWords = 0
        var totalCharacters = 0
        var totalTimeMs = 0L
        var longestNote: Note? = null
        val wordsByDay = HashMap<LocalDate, Int>()

        notes.forEach { note ->
            val words = NoteUtils.countWords(note.title) + NoteUtils.countWords(note.content)
            val chars = NoteUtils.countCharacters(note.title) + NoteUtils.countCharacters(note.content)
            totalWords += words
            totalCharacters += chars
            totalTimeMs += note.totalTimeMs
            if (longestNote == null || note.totalTimeMs > longestNote.totalTimeMs) {
                longestNote = note
            }
            val day = LocalDate.ofInstant(Instant.ofEpochMilli(note.createdTime), zone)
            wordsByDay[day] = (wordsByDay[day] ?: 0) + words
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

        val lastActive = runCatching { LocalDate.parse(prefs.lastActiveDate) }.getOrNull()
        val streak = if (lastActive == null) {
            0
        } else if (lastActive == now || lastActive == now.minusDays(1)) {
            prefs.streakDays
        } else {
            0
        }

        val activeDays = wordsByDay.values.count { it > 0 }
        val avg = if (activeDays > 0) totalWords / activeDays else 0

        val categorySlices = notes
            .groupBy { it.category.ifBlank { "Tanpa Kategori" } }
            .map { (label, list) -> CategorySlice(label, list.size) }
            .sortedByDescending { it.count }

        return StatsUiState(
            period = period,
            totalWords = totalWords,
            totalCharacters = totalCharacters,
            totalNotes = notes.size,
            avgWordsPerDay = avg,
            streakDays = streak,
            totalTimeMs = totalTimeMs,
            longestNoteTitle = longestNote?.title?.ifBlank { "Tanpa Judul" } ?: "-",
            longestNoteTimeMs = longestNote?.totalTimeMs ?: 0L,
            buckets = buckets,
            categorySlices = categorySlices
        )
    }
}