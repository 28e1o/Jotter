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

package com.openappslabs.jotter.ui.screens.aboutscreen

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openappslabs.jotter.data.repository.NotesRepository
import com.openappslabs.jotter.utils.NoteUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class WritingStats(
    val totalNotes: Int = 0,
    val totalWords: Int = 0,
    val totalCharacters: Int = 0,
    val streakDays: Int = 0
)

@HiltViewModel
class AboutScreenViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {

    val stats: StateFlow<WritingStats> = flow {
        val activeNotes = notesRepository.getAllNotesSync().filter { !it.isTrashed }

        var words = 0
        var characters = 0
        val days = mutableSetOf<LocalDate>()
        val zone = ZoneId.systemDefault()

        activeNotes.forEach { note ->
            words += NoteUtils.countWords(note.title) + NoteUtils.countWords(note.content)
            characters += note.title.length + note.content.length
            days.add(LocalDate.ofInstant(Instant.ofEpochMilli(note.createdTime), zone))
            days.add(LocalDate.ofInstant(Instant.ofEpochMilli(note.updatedTime), zone))
        }

        emit(
            WritingStats(
                totalNotes = activeNotes.size,
                totalWords = words,
                totalCharacters = characters,
                streakDays = currentStreak(days)
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = WritingStats()
    )

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