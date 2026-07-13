/*
 * Copyright (c) 2026 Vinilo Project
 * EditableSongTags.kt is part of Vinilo.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.anderson.vinilo.music.tagediting

import org.oxycblt.musikr.Song
import org.oxycblt.musikr.tag.Name

data class EditableSongTags(
    val title: String,
    val artists: List<String>,
    val genres: List<String>,
)

fun List<String>.joinForDisplay(): String = joinToString("; ")

fun String.splitToTagValues(): List<String> =
    split(";", ",").map { it.trim() }.filter { it.isNotEmpty() }

/** Falls back to musikr's already-resolved values if there's nothing editable yet. */
fun EditableSongTags?.orFallback(song: Song): EditableSongTags =
    this?.takeIf { it.title.isNotBlank() }
        ?: EditableSongTags(
            title = song.name.raw,
            artists = song.artists.mapNotNull { (it.name as? Name.Known)?.raw },
            genres = song.genres.mapNotNull { (it.name as? Name.Known)?.raw },
        )
