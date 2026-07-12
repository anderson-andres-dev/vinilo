/*
 * Copyright (c) 2026 Vinilo Project
 * MusicRepository.kt is part of Vinilo.
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

package com.anderson.vinilo.music

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.oxycblt.musikr.Config
import org.oxycblt.musikr.Interpretation
import org.oxycblt.musikr.MutableLibrary
import org.oxycblt.musikr.Musikr
import org.oxycblt.musikr.Storage
import org.oxycblt.musikr.cache.db.MutableDBCache
import org.oxycblt.musikr.covers.embedded.CoverIdentifier
import org.oxycblt.musikr.covers.embedded.EmbeddedCovers
import org.oxycblt.musikr.covers.stored.CoverStorage
import org.oxycblt.musikr.covers.stored.MutableStoredCovers
import org.oxycblt.musikr.covers.stored.NoTranscoding
import org.oxycblt.musikr.fs.Location
import org.oxycblt.musikr.fs.saf.SAF
import org.oxycblt.musikr.playlist.db.StoredPlaylists
import org.oxycblt.musikr.tag.interpret.Naming
import org.oxycblt.musikr.tag.interpret.Separators

/**
 * Bootstrap-phase wrapper around [Musikr]. Only knows how to scan a single SAF folder chosen by
 * the user and hold the resulting library in memory; no cache-aware incremental re-scan, location
 * management, or playlist mutation yet.
 */
@Singleton
class MusicRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val _library = MutableStateFlow<MutableLibrary?>(null)
    val library: StateFlow<MutableLibrary?> = _library.asStateFlow()

    private val _indexing = MutableStateFlow(false)
    val indexing: StateFlow<Boolean> = _indexing.asStateFlow()

    suspend fun scan(folderUri: Uri) {
        _indexing.value = true
        try {
            val location =
                requireNotNull(Location.Unopened.from(context, folderUri).open(context)) {
                    "Could not obtain persistable access to $folderUri"
                }
            val fs =
                SAF.from(
                    context,
                    SAF.Query(
                        source = listOf(location),
                        exclude = emptyList(),
                        withHidden = false,
                        multithread = true,
                    ),
                )
            val storage =
                Storage(
                    cache = MutableDBCache.from(context),
                    covers = embeddedOnlyCovers(),
                    storedPlaylists = StoredPlaylists.from(context),
                )
            val interpretation =
                Interpretation(naming = Naming.simple(), separators = Separators.from(";"))
            val result = Musikr.new(context, Config(fs, storage, interpretation)).run()
            _library.value = result.library
            result.cleanup()
        } finally {
            _indexing.value = false
        }
    }

    private suspend fun embeddedOnlyCovers() =
        MutableStoredCovers(
            src = EmbeddedCovers(CoverIdentifier.md5()),
            coverStorage = CoverStorage.at(context.filesDir.resolve("covers").apply { mkdirs() }),
            transcoding = NoTranscoding,
        )
}
