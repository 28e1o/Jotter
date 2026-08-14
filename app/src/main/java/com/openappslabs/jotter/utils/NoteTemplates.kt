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

data class NoteTemplate(
    val name: String,
    val title: String,
    val content: String
)

object NoteTemplates {

    val task: NoteTemplate = NoteTemplate(
        name = "Daftar Tugas",
        title = "Daftar Tugas",
        content = """
            - [ ] Tugas pertama
            - [ ] Tugas kedua
            - [ ] Tugas ketiga
            - [ ] Tugas keempat
        """.trimIndent()
    )

    val journal: NoteTemplate = NoteTemplate(
        name = "Jurnal",
        title = "Jurnal",
        content = """
            # Jurnal

            **Tanggal:**

            **Perasaan hari ini:**

            **Hal yang terjadi:**

            **Hal yang ingin kucapai:**
        """.trimIndent()
    )

    val meeting: NoteTemplate = NoteTemplate(
        name = "Rapat",
        title = "Rapat",
        content = """
            # Rapat

            **Topik:**

            **Peserta:**

            - [ ] Poin pembahasan 1
            - [ ] Poin pembahasan 2
            - [ ] Poin pembahasan 3

            **Tindak lanjut:**
        """.trimIndent()
    )

    val idea: NoteTemplate = NoteTemplate(
        name = "Ide",
        title = "Ide",
        content = """
            # Ide

            **Deskripsi:**

            **Langkah selanjutnya:**
        """.trimIndent()
    )

    val dailyNote: NoteTemplate = NoteTemplate(
        name = "Catatan Harian",
        title = "Catatan Harian",
        content = """
            # Catatan Harian

            **Tanggal:**

            **Hari ini:**

            **Bersyukur atas:**

            **Besok:**
        """.trimIndent()
    )

    val all: List<NoteTemplate> = listOf(task, journal, meeting, idea, dailyNote)
}
