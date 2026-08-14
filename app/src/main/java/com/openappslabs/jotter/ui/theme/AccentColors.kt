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

package com.openappslabs.jotter.ui.theme

data class AccentPreset(
    val name: String,
    val hex: String
)

val AccentPresets = listOf(
    AccentPreset("Biru", "#1E88E5"),
    AccentPreset("Hijau", "#43A047"),
    AccentPreset("Ungu", "#8E24AA"),
    AccentPreset("Oranye", "#FB8C00"),
    AccentPreset("Merah", "#E53935"),
    AccentPreset("Merah Muda", "#D81B60"),
    AccentPreset("Teal", "#00897B"),
    AccentPreset("Biru Laut", "#3949AB")
)