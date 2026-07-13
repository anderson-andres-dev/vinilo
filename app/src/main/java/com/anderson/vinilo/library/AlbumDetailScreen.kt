/*
 * Copyright (c) 2026 Vinilo Project
 * AlbumDetailScreen.kt is part of Vinilo.
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.anderson.vinilo.ui.PlayingIndicator
import com.anderson.vinilo.ui.SongListItem
import com.anderson.vinilo.ui.display
import com.anderson.vinilo.ui.formatDuration
import com.anderson.vinilo.ui.pluralize
import org.oxycblt.musikr.Album
import org.oxycblt.musikr.Music

@Composable
fun AlbumDetailScreen(
    uid: Music.UID,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val library by libraryViewModel.library.collectAsStateWithLifecycle()
    val album = library?.findAlbum(uid)
    LaunchedEffect(library) {
        if (library != null && album == null) onBack()
    }
    if (album == null) return

    val songs = album.songs.sortedWith(compareBy({ it.disc?.number ?: 0 }, { it.track ?: Int.MAX_VALUE }))
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CoverThumbnail(cover = album.covers.covers.firstOrNull(), size = 200.dp, cornerRadius = 20.dp)
            }
            Text(
                text = "Álbum",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = album.name.display(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
            )
            Text(
                text = albumMetaLine(album),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(songs, key = { it.uid }) { song ->
                val isCurrent = playbackState?.song?.uid == song.uid
                SongListItem(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && playbackState?.isPlaying == true,
                    onClick = { playbackViewModel.play(songs, songs.indexOf(song)) },
                    showTrailingIndicator = false,
                    leading = {
                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            if (isCurrent) {
                                PlayingIndicator(isPlaying = playbackState?.isPlaying == true)
                            } else {
                                Text(text = "${song.track ?: '-'}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun albumMetaLine(album: Album): String {
    val artist = album.artists.joinToString { it.name.display() }.ifEmpty { "Artista desconocido" }
    val year = album.dates?.min?.year
    return listOfNotNull(
            artist,
            year?.toString(),
            pluralize(album.songs.size, "canción", "canciones"),
            album.durationMs.formatDuration(),
        )
        .joinToString(" · ")
}
