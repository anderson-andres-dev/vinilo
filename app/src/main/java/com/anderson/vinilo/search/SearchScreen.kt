/*
 * Copyright (c) 2026 Vinilo Project
 * SearchScreen.kt is part of Vinilo.
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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anderson.vinilo.library.AlbumRow
import com.anderson.vinilo.library.ArtistRow
import com.anderson.vinilo.library.GenreRow
import com.anderson.vinilo.library.PlaylistRow
import com.anderson.vinilo.playback.PlaybackViewModel
import com.anderson.vinilo.ui.SongListItem
import org.oxycblt.musikr.Music

private val TYPE_LABELS =
    mapOf(
        SearchType.SONG to "Canciones",
        SearchType.ALBUM to "Álbumes",
        SearchType.ARTIST to "Artistas",
        SearchType.GENRE to "Géneros",
        SearchType.PLAYLIST to "Playlists",
    )

@Composable
fun SearchScreen(
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (Music.UID) -> Unit,
    onOpenArtist: (Music.UID) -> Unit,
    onOpenGenre: (Music.UID) -> Unit,
    onOpenPlaylist: (Music.UID) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val activeTypes by viewModel.activeTypes.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val hiddenSongs by viewModel.hiddenSongs.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text("Buscar en tu biblioteca") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Limpiar")
                        }
                    }
                },
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (type in SearchType.entries) {
                FilterChip(
                    selected = type in activeTypes,
                    onClick = { viewModel.onToggleType(type) },
                    label = { Text(TYPE_LABELS.getValue(type)) },
                )
            }
        }

        when {
            query.isBlank() ->
                Text(
                    text = "Escribe para buscar en tu biblioteca.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            results.isEmpty ->
                Text(
                    text = "Sin resultados para \"$query\".",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            else ->
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (results.artists.isNotEmpty()) {
                        item { SectionHeader("Artistas") }
                        items(results.artists, key = { it.uid }) { artist ->
                            PaddedRow {
                                ArtistRow(artist = artist, hiddenSongs = hiddenSongs, onClick = { onOpenArtist(artist.uid) })
                            }
                        }
                    }
                    if (results.albums.isNotEmpty()) {
                        item { SectionHeader("Álbumes") }
                        items(results.albums, key = { it.uid }) { album ->
                            PaddedRow {
                                AlbumRow(album = album, hiddenSongs = hiddenSongs, onClick = { onOpenAlbum(album.uid) })
                            }
                        }
                    }
                    if (results.genres.isNotEmpty()) {
                        item { SectionHeader("Géneros") }
                        items(results.genres, key = { it.uid }) { genre ->
                            PaddedRow {
                                GenreRow(genre = genre, hiddenSongs = hiddenSongs, onClick = { onOpenGenre(genre.uid) })
                            }
                        }
                    }
                    if (results.songs.isNotEmpty()) {
                        item { SectionHeader("Canciones") }
                        itemsIndexed(results.songs, key = { _, song -> song.uid }) { index, song ->
                            val isCurrent = playbackState?.song?.uid == song.uid
                            SongListItem(
                                song = song,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && playbackState?.isPlaying == true,
                                onClick = { playbackViewModel.play(results.songs, index) },
                            )
                        }
                    }
                    if (results.playlists.isNotEmpty()) {
                        item { SectionHeader("Playlists") }
                        items(results.playlists, key = { it.uid }) { playlist ->
                            PaddedRow {
                                PlaylistRow(
                                    playlist = playlist,
                                    hiddenSongs = hiddenSongs,
                                    onClick = { onOpenPlaylist(playlist.uid) },
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun PaddedRow(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) { content() }
}
