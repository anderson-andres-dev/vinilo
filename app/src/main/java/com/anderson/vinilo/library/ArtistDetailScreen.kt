/*
 * Copyright (c) 2026 Vinilo Project
 * ArtistDetailScreen.kt is part of Vinilo.
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

package com.anderson.vinilo.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anderson.vinilo.playback.PlaybackViewModel
import com.anderson.vinilo.ui.CoverThumbnail
import com.anderson.vinilo.ui.SongListItem
import com.anderson.vinilo.ui.display
import com.anderson.vinilo.ui.formatDuration
import com.anderson.vinilo.ui.pluralize
import org.oxycblt.musikr.Music

@Composable
fun ArtistDetailScreen(
    uid: Music.UID,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (Music.UID) -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val library by libraryViewModel.library.collectAsStateWithLifecycle()
    val artist = library?.findArtist(uid)
    LaunchedEffect(library) {
        if (library != null && artist == null) onBack()
    }
    if (artist == null) return

    val songs = artist.songs.sortedBy { it.name.raw }
    val albums = artistAlbums(artist).sortedBy { it.name.display() }
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CoverThumbnail(cover = artist.covers.covers.firstOrNull(), size = 200.dp, cornerRadius = 20.dp)
                }
                Text(
                    text = "Artista",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = artist.name.display(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                )
                Text(
                    text =
                        "${pluralize(albums.size, "álbum", "álbumes")} · " +
                            "${pluralize(songs.size, "canción", "canciones")} · " +
                            (artist.durationMs ?: 0).formatDuration(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { playbackViewModel.play(songs, 0) }, modifier = Modifier.height(36.dp)) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        Text(text = " Reproducir")
                    }
                    Button(
                        onClick = { playbackViewModel.play(songs.shuffled(), 0) },
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.Shuffle, contentDescription = null)
                        Text(text = " Aleatorio")
                    }
                }
            }
            if (albums.isNotEmpty()) {
                Text(
                    text = "Álbumes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
        }
        items(albums, key = { it.uid }) { album ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                AlbumRow(album = album, onClick = { onOpenAlbum(album.uid) })
            }
        }
        item {
            Text(
                text = "Canciones",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
        items(songs, key = { it.uid }) { song ->
            val isCurrent = playbackState?.song?.uid == song.uid
            SongListItem(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isCurrent && playbackState?.isPlaying == true,
                onClick = { playbackViewModel.play(songs, songs.indexOf(song)) },
            )
        }
    }
}
