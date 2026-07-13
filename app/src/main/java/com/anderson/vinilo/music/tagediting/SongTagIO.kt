/*
 * Copyright (c) 2026 Vinilo Project
 * SongTagIO.kt is part of Vinilo.
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

import android.content.Context
import android.net.Uri
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the current title/artist/genre tags directly from the file via [TagLib] (`io.github.kyant0:taglib`,
 * a separate write-capable JNI wrapper -- musikr's own vendored TagLib is read-only). Returns null
 * if the file can't be read or has no usable metadata; callers should fall back to musikr's
 * already-resolved values (see [orFallback]) rather than showing a blank form.
 */
suspend fun readSongTags(context: Context, uri: Uri): EditableSongTags? =
    withContext(Dispatchers.IO) {
        runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val metadata = TagLib.getMetadata(pfd.dup().detachFd(), readPictures = false) ?: return@use null
                    EditableSongTags(
                        title = metadata.propertyMap["TITLE"]?.firstOrNull().orEmpty(),
                        artists = metadata.propertyMap["ARTIST"]?.toList().orEmpty(),
                        genres = metadata.propertyMap["GENRE"]?.toList().orEmpty(),
                    )
                }
            }
            .getOrNull()
    }

/**
 * Writes title/artist/genre back to the file. [uri] must already have resolved write access (see
 * `requestWriteAccess` in `edit/WritableUriAccess.kt`) -- this function does not handle permission
 * flows. Returns whether TagLib reports the save as successful; never throws for a plain failed
 * save (only for I/O errors opening the descriptor), and never leaves the file partially written --
 * `TagLib.savePropertyMap` is a single atomic native call.
 */
suspend fun writeSongTags(context: Context, uri: Uri, tags: EditableSongTags): Boolean =
    withContext(Dispatchers.IO) {
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
            val propertyMap =
                hashMapOf(
                    "TITLE" to arrayOf(tags.title),
                    "ARTIST" to tags.artists.toTypedArray(),
                    "GENRE" to tags.genres.toTypedArray(),
                )
            TagLib.savePropertyMap(pfd.dup().detachFd(), propertyMap)
        } ?: false
    }
