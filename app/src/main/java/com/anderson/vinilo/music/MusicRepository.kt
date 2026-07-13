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
import com.anderson.vinilo.settings.LibrarySettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import org.oxycblt.musikr.fs.FS
import org.oxycblt.musikr.fs.File
import org.oxycblt.musikr.fs.Location
import org.oxycblt.musikr.fs.mediastore.MediaStore as MusikrMediaStore
import org.oxycblt.musikr.fs.saf.SAF
import org.oxycblt.musikr.playlist.db.StoredPlaylists
import org.oxycblt.musikr.tag.interpret.Naming
import org.oxycblt.musikr.tag.interpret.Separators

/**
 * Folder-name / Android package-id fragments (case-insensitive substring match) that mark a
 * file as coming from a messaging app rather than being real music -- voice notes and forwarded
 * clips, not songs. Package ids (e.g. "com.whatsapp") already contain the human-readable name,
 * so one list covers both. Only applied to the automatic MediaStore-wide scan -- a custom folder
 * the user deliberately adds in Settings is never filtered.
 */
private val MESSAGING_APP_KEYWORDS =
    listOf(
        "whatsapp",
        "telegram",
        "signal",
        "viber",
        "wechat",
        "skype",
        "discord",
        "threema",
        "imo",
        "com.facebook.orca", // Facebook Messenger
    )

private fun isFromMessagingApp(file: File): Boolean =
    file.path.components.components.any { segment ->
        MESSAGING_APP_KEYWORDS.any { keyword -> segment.contains(keyword, ignoreCase = true) }
    }

/**
 * Wrapper around [Musikr]. The default source is the device-wide MediaStore audio index (no
 * folder picking required, mirroring how other music players surface existing audio); any SAF
 * folders the user adds manually via [addCustomFolder] are persisted via [librarySettings] and
 * merged into every scan on top of that default via [CompositeFS]. MediaStore here only
 * discovers *which* files exist -- TagLib (via musikr's own extract step) still reads every
 * file's actual tags, so this doesn't trade away metadata richness.
 */
@Singleton
class MusicRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val librarySettings: LibrarySettingsRepository,
) {
    private val _library = MutableStateFlow<MutableLibrary?>(null)
    val library: StateFlow<MutableLibrary?> = _library.asStateFlow()

    private val _indexing = MutableStateFlow(false)
    val indexing: StateFlow<Boolean> = _indexing.asStateFlow()

    private val scanMutex = Mutex()

    val customFolderUris: Flow<Set<Uri>> = librarySettings.customFolderUris

    suspend fun addCustomFolder(uri: Uri) {
        librarySettings.addCustomFolder(uri)
        rescan()
    }

    suspend fun removeCustomFolder(uri: Uri) {
        librarySettings.removeCustomFolder(uri)
        rescan()
    }

    suspend fun rescan() =
        scanMutex.withLock {
            _indexing.value = true
            try {
                val sources = mutableListOf<FS>(deviceMediaStore())
                for (uri in librarySettings.customFolderUris.first()) {
                    val opened = Location.Unopened.from(context, uri).open(context)
                    if (opened == null) {
                        // Permission was revoked outside the app (e.g. via system settings).
                        librarySettings.removeCustomFolder(uri)
                        continue
                    }
                    sources.add(
                        SAF.from(
                            context,
                            SAF.Query(
                                source = listOf(opened),
                                exclude = emptyList(),
                                withHidden = false,
                                multithread = true,
                            ),
                        )
                    )
                }
                val fs = CompositeFS(sources)
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

    private fun deviceMediaStore(): FS =
        FilteringFS(
            MusikrMediaStore.from(
                context,
                MusikrMediaStore.Query(
                    mode = MusikrMediaStore.FilterMode.EXCLUDE,
                    filtered = emptyList(),
                    excludeNonMusic = true,
                ),
            )
        ) { file -> !isFromMessagingApp(file) }

    private suspend fun embeddedOnlyCovers() =
        MutableStoredCovers(
            src = EmbeddedCovers(CoverIdentifier.md5()),
            coverStorage = CoverStorage.at(context.filesDir.resolve("covers").apply { mkdirs() }),
            transcoding = NoTranscoding,
        )
}
