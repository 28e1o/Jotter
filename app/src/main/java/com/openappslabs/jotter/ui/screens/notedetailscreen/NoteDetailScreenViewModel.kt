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

package com.openappslabs.jotter.ui.screens.notedetailscreen

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.openappslabs.jotter.data.model.Note
import com.openappslabs.jotter.data.repository.CategoryRepository
import com.openappslabs.jotter.data.repository.NotesRepository
import com.openappslabs.jotter.data.repository.UserPreferences
import com.openappslabs.jotter.data.repository.UserPreferencesRepository
import com.openappslabs.jotter.navigation.AppRoutes
import com.openappslabs.jotter.utils.ActiveFormat
import com.openappslabs.jotter.utils.RichFont
import com.openappslabs.jotter.utils.applyFormat
import com.openappslabs.jotter.utils.buildAnnotated
import com.openappslabs.jotter.utils.decodeSpans
import com.openappslabs.jotter.utils.editSpans
import com.openappslabs.jotter.utils.encodeSpans
import com.openappslabs.jotter.utils.lineIndexForOffset
import com.openappslabs.jotter.utils.lineStartOffsets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_UNDO_STEPS = 50

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notesRepository: NotesRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppRoutes.NoteDetail>()
    private val noteId = route.noteId
    private val passedCategory = route.category
    private val templateTitle = route.templateTitle
    private val templateContent = route.templateContent

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, UserPreferences())

    @Immutable
    data class UiState(
        val id: Int? = null,
        val title: String = "",
        val content: String = "",
        val contentAnnotations: String = "",
        val category: String = "",
        val isPinned: Boolean = false,
        val isLocked: Boolean = false,
        val isArchived: Boolean = false,
        val isTrashed: Boolean = false,
        val createdTime: Long = System.currentTimeMillis(),
        val lastEdited: Long = System.currentTimeMillis(),
        val isNotePersisted: Boolean = false,
        val isLoading: Boolean = true,
        val isModified: Boolean = false,
        val canUndo: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _contentEditor = MutableStateFlow(TextFieldValue(""))
    val contentEditor: StateFlow<TextFieldValue> = _contentEditor.asStateFlow()

    private val _activeFormat = MutableStateFlow(ActiveFormat())
    val activeFormat: StateFlow<ActiveFormat> = _activeFormat.asStateFlow()

    private var originalState: UiState? = null

    private data class Snapshot(
        val title: String,
        val content: String,
        val contentAnnotations: String,
        val category: String
    )

    private val undoHistory = ArrayDeque<Snapshot>()

    val availableCategories: StateFlow<List<String>> = categoryRepository.getAllCategories()
        .map { categoryList -> categoryList.map { it.name } }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        if (noteId != -1) {
            loadNote(noteId)
        } else {
            val initialState = UiState(
                title = templateTitle ?: "",
                content = templateContent ?: "",
                category = passedCategory ?: "",
                isNotePersisted = false,
                isLoading = false
            )
            _uiState.update { initialState }
            originalState = initialState
            syncEditorFromState()
        }
        observeCategoryCleanup()
        observeNoteUpdates()
    }

    private fun observeCategoryCleanup() {
        viewModelScope.launch {
            availableCategories.drop(1).collectLatest { categories ->
                val currentCategory = uiState.value.category
                if (currentCategory.isNotBlank() && !categories.contains(currentCategory)) {
                    _uiState.update { it.copy(category = "") }
                    checkForChanges()
                }
            }
        }
    }

    private fun observeNoteUpdates() {
        if (noteId != -1) {
            viewModelScope.launch {
                notesRepository.getAllNotes().collectLatest { notes ->
                    val updatedNote = notes.find { it.id == noteId }
                    if (updatedNote != null && updatedNote.category != uiState.value.category) {
                        _uiState.update { it.copy(category = updatedNote.category) }
                        originalState = originalState?.copy(category = updatedNote.category)
                        checkForChanges()
                    }
                }
            }
        }
    }

    private fun loadNote(id: Int) {
        viewModelScope.launch {
            val note = notesRepository.getNoteById(id)
            if (note != null) {
                val newState = UiState(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    contentAnnotations = note.contentAnnotations,
                    category = note.category,
                    isPinned = note.isPinned,
                    isLocked = note.isLocked,
                    isArchived = note.isArchived,
                    isTrashed = note.isTrashed,
                    createdTime = note.createdTime,
                    lastEdited = note.updatedTime,
                    isNotePersisted = true,
                    isLoading = false,
                    isModified = false
                )
                _uiState.update { newState }
                originalState = newState
                clearUndoHistory()
                syncEditorFromState()
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveNoteStatus() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState.id != null) {
                val updatedNote = Note(
                    id = currentState.id,
                    title = currentState.title,
                    content = currentState.content,
                    contentAnnotations = currentState.contentAnnotations,
                    category = currentState.category,
                    isPinned = currentState.isPinned,
                    isLocked = currentState.isLocked,
                    isArchived = currentState.isArchived,
                    isTrashed = currentState.isTrashed,
                    createdTime = currentState.createdTime,
                    updatedTime = System.currentTimeMillis()
                )
                notesRepository.updateNote(updatedNote)
            }
        }
    }

    private fun checkForChanges() {
        val current = _uiState.value
        val original = originalState ?: return
        val modified = current.title != original.title ||
                       current.content != original.content ||
                       current.contentAnnotations != original.contentAnnotations ||
                       current.category != original.category
        
        if (current.isModified != modified) {
            _uiState.update { it.copy(isModified = modified) }
        }
    }

    private fun syncEditorFromState() {
        val state = _uiState.value
        _contentEditor.value = TextFieldValue(
            annotatedString = buildAnnotated(state.content, decodeSpans(state.contentAnnotations)),
            selection = TextRange(state.content.length),
            composition = null
        )
        _activeFormat.value = ActiveFormat()
    }

    private fun pushSnapshot() {
        val current = _uiState.value
        val snapshot = Snapshot(current.title, current.content, current.contentAnnotations, current.category)
        if (undoHistory.lastOrNull() == snapshot) return
        undoHistory.addLast(snapshot)
        while (undoHistory.size > MAX_UNDO_STEPS) {
            undoHistory.removeFirst()
        }
        _uiState.update { it.copy(canUndo = true) }
    }

    private fun clearUndoHistory() {
        undoHistory.clear()
        _uiState.update { it.copy(canUndo = false) }
    }

    fun undo() {
        val snapshot = undoHistory.removeLastOrNull() ?: return
        _uiState.update {
            it.copy(
                title = snapshot.title,
                content = snapshot.content,
                contentAnnotations = snapshot.contentAnnotations,
                category = snapshot.category,
                canUndo = undoHistory.isNotEmpty()
            )
        }
        syncEditorFromState()
        checkForChanges()
    }

    fun toggleChecklist(line: Int, checked: Boolean) {
        val lines = _uiState.value.content.split("\n").toMutableList()
        if (line !in lines.indices) return
        val old = lines[line]
        val updated = if (checked) {
            old.replaceFirst("- [ ] ", "- [x] ")
        } else {
            old.replaceFirst("- [x] ", "- [ ] ")
        }
        if (updated == old) return
        pushSnapshot()
        lines[line] = updated
        _uiState.update { it.copy(content = lines.joinToString("\n")) }
        checkForChanges()
    }

    fun updateTitle(newTitle: String) {
        pushSnapshot()
        _uiState.update { it.copy(title = newTitle) }
        checkForChanges()
    }

    fun updateContent(newValue: TextFieldValue) {
        val oldEditor = _contentEditor.value
        val oldText = oldEditor.text
        val newText = newValue.annotatedString.text
        val textChanged = newText != oldText

        var newSpans = decodeSpans(uiState.value.contentAnnotations)
        if (textChanged) {
            var commonPrefix = 0
            while (commonPrefix < oldText.length && commonPrefix < newText.length &&
                oldText[commonPrefix] == newText[commonPrefix]
            ) {
                commonPrefix++
            }
            var commonSuffix = 0
            while (commonSuffix < oldText.length - commonPrefix &&
                commonSuffix < newText.length - commonPrefix &&
                oldText[oldText.length - 1 - commonSuffix] == newText[newText.length - 1 - commonSuffix]
            ) {
                commonSuffix++
            }
            val oldModifiedEnd = oldText.length - commonSuffix
            val newModifiedEnd = newText.length - commonSuffix
            newSpans = editSpans(newSpans, commonPrefix, oldModifiedEnd, newModifiedEnd - commonPrefix)
            if (newModifiedEnd > commonPrefix) {
                newSpans = applyFormat(newSpans, commonPrefix, newModifiedEnd, _activeFormat.value)
            }
        }

        _contentEditor.value = newValue.copy(annotatedString = buildAnnotated(newText, newSpans))
        if (textChanged) {
            pushSnapshot()
            _uiState.update {
                it.copy(
                    content = newText,
                    contentAnnotations = encodeSpans(newSpans)
                )
            }
            checkForChanges()
        }
    }

    private fun applyActiveFormatToSelection(newFormat: ActiveFormat) {
        val editor = _contentEditor.value
        val selection = editor.selection
        if (selection.end > selection.start) {
            val newSpans = applyFormat(
                decodeSpans(uiState.value.contentAnnotations),
                selection.start,
                selection.end,
                newFormat
            )
            _contentEditor.value = editor.copy(annotatedString = buildAnnotated(editor.text, newSpans))
            pushSnapshot()
            _uiState.update { it.copy(contentAnnotations = encodeSpans(newSpans)) }
            checkForChanges()
        }
    }

    fun toggleBold() {
        val newFormat = _activeFormat.value.copy(bold = !_activeFormat.value.bold)
        _activeFormat.value = newFormat
        applyActiveFormatToSelection(newFormat)
    }

    fun toggleItalic() {
        val newFormat = _activeFormat.value.copy(italic = !_activeFormat.value.italic)
        _activeFormat.value = newFormat
        applyActiveFormatToSelection(newFormat)
    }

    fun setEditorFont(font: RichFont) {
        val newFormat = _activeFormat.value.copy(font = font)
        _activeFormat.value = newFormat
        applyActiveFormatToSelection(newFormat)
    }

    fun toggleBulletList() {
        val editor = _contentEditor.value
        val text = editor.text
        val selection = editor.selection
        if (text.isEmpty()) return

        var newText = text
        var newSpans = decodeSpans(uiState.value.contentAnnotations)

        val startLine = lineIndexForOffset(text, selection.start)
        val endLine = lineIndexForOffset(text, selection.end)
        for (line in endLine downTo startLine) {
            val offsets = lineStartOffsets(newText)
            if (line >= offsets.size) continue
            val lineStart = offsets[line]
            val lineEnd = if (line + 1 < offsets.size) offsets[line + 1] - 1 else newText.length
            if (lineEnd <= lineStart) continue
            val lineText = newText.substring(lineStart, lineEnd)
            if (lineText.startsWith("- ")) {
                newText = newText.removeRange(lineStart, lineStart + 2)
                newSpans = editSpans(newSpans, lineStart, lineStart + 2, 0)
            } else {
                newText = newText.substring(0, lineStart) + "- " + newText.substring(lineStart)
                newSpans = editSpans(newSpans, lineStart, lineStart, 2)
            }
        }

        _contentEditor.value = TextFieldValue(
            annotatedString = buildAnnotated(newText, newSpans),
            selection = TextRange(selection.start.coerceAtMost(newText.length)),
            composition = editor.composition
        )
        pushSnapshot()
        _uiState.update {
            it.copy(
                content = newText,
                contentAnnotations = encodeSpans(newSpans)
            )
        }
        checkForChanges()
    }

    fun updateCategory(newCategory: String) {
        pushSnapshot()
        _uiState.update { it.copy(category = newCategory) }
        checkForChanges()
    }

    fun togglePin() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
        saveNoteStatus()
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
        saveNoteStatus()
    }

    fun saveNote() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.category.isNotBlank()) {
                categoryRepository.insertCategory(currentState.category)
            }

            val noteToSave = Note(
                id = currentState.id ?: 0,
                title = currentState.title,
                content = currentState.content,
                contentAnnotations = currentState.contentAnnotations,
                category = currentState.category,
                isPinned = currentState.isPinned,
                isLocked = currentState.isLocked,
                isArchived = currentState.isArchived,
                isTrashed = currentState.isTrashed
            )

            if (currentState.isNotePersisted) {
                notesRepository.updateNote(noteToSave)
                loadNote(currentState.id!!)
            } else {
                val newId = notesRepository.addNote(noteToSave).toInt()
                loadNote(newId)
            }
            clearUndoHistory()
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            val note = uiState.value
            val noteToDelete = Note(id = note.id ?: 0)
            if (note.isTrashed) {
                notesRepository.deleteNote(noteToDelete)
            } else {
                notesRepository.trashNote(noteToDelete)
            }
        }
    }

    fun archiveNote() {
        viewModelScope.launch {
            notesRepository.archiveNote(Note(id = uiState.value.id ?: 0))
        }
    }

    fun restoreNote() {
        viewModelScope.launch {
            notesRepository.restoreNote(Note(id = uiState.value.id ?: 0))
        }
    }

    fun undoChanges() {
        if (noteId != -1) {
            loadNote(noteId)
        }
    }

    fun duplicateNote(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val current = uiState.value
            val newTitle = if (current.title.isNotBlank()) "Salinan dari ${current.title}" else "Salinan dari Tanpa Judul"
            val duplicatedNote = Note(
                id = 0,
                title = newTitle,
                content = current.content,
                contentAnnotations = current.contentAnnotations,
                category = current.category,
                isPinned = false,
                isLocked = false,
                isArchived = false,
                isTrashed = false
            )
            val newId = notesRepository.addNote(duplicatedNote).toInt()
            onComplete(newId)
        }
    }
}