/*
 * Copyright (c) 2026 Vinilo Project
 * SearchViewModel.kt is part of Vinilo.
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

package com.anderson.vinilo.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anderson.vinilo.music.MusicRepository
import com.anderson.vinilo.music.excludingHidden
import com.anderson.vinilo.settings.LibrarySettingsRepository
import com.anderson.vinilo.ui.display
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.oxycblt.musikr.Album
import org.oxycblt.musikr.Artist
import org.oxycblt.musikr.Genre
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.Playlist
import org.oxycblt.musikr.Song

enum class SearchType {
    ARTIST,
    ALBUM,
    GENRE,
    SONG,
    PLAYLIST,
}

data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmpty
        get() =
            artists.isEmpty() &&
                albums.isEmpty() &&
                genres.isEmpty() &&
                songs.isEmpty() &&
                playlists.isEmpty()

    companion object {
        val EMPTY = SearchResults()
    }
}

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val musicRepository: MusicRepository,
    private val librarySettings: LibrarySettingsRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _activeTypes = MutableStateFlow(SearchType.entries.toSet())
    val activeTypes: StateFlow<Set<SearchType>> = _activeTypes.asStateFlow()

    val hiddenSongs: StateFlow<Set<Music.UID>> =
        librarySettings.hiddenSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Recomputing a filter{} pass over Collection<Song/Album/...> on every keystroke is fine at
    // this app's scale (hundreds to low thousands of local files) -- no debounce needed.
    val results: StateFlow<SearchResults> =
        combine(musicRepository.library, _query, _activeTypes, librarySettings.hiddenSongs) {
                library,
                query,
                types,
                hiddenSongs ->
                if (library == null || query.isBlank()) {
                    SearchResults.EMPTY
                } else {
                    SearchResults(
                        artists =
                            if (SearchType.ARTIST in types) library.artists.matching(query)
                            else emptyList(),
                        albums =
                            if (SearchType.ALBUM in types) library.albums.matching(query)
                            else emptyList(),
                        genres =
                            if (SearchType.GENRE in types) library.genres.matching(query)
                            else emptyList(),
                        songs =
                            if (SearchType.SONG in types) {
                                library.songs.matching(query).excludingHidden(hiddenSongs)
                            } else {
                                emptyList()
                            },
                        playlists =
                            if (SearchType.PLAYLIST in types) library.playlists.matching(query)
                            else emptyList(),
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults.EMPTY)

    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun onToggleType(type: SearchType) {
        _activeTypes.value =
            _activeTypes.value.let { if (type in it) it - type else it + type }
    }
}

private fun <T : Music> Collection<T>.matching(query: String): List<T> =
    filter { it.name.display().contains(query, ignoreCase = true) }
