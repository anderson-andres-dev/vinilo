/*
 * Copyright (c) 2026 Vinilo Project
 * SongsTab.kt is part of Vinilo.
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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anderson.vinilo.playback.PlaybackViewModel
import com.anderson.vinilo.ui.SwipeableSongRow
import kotlinx.coroutines.launch
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.Song

@Composable
fun SongsTab(
    songs: List<Song>,
    indexing: Boolean,
    playbackViewModel: PlaybackViewModel,
    swipeGesturesSwapped: Boolean,
    snackbarHostState: SnackbarHostState,
    onEditSong: (Music.UID) -> Unit,
    onSetSongHidden: (Music.UID, Boolean) -> Unit,
) {
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (songs.isEmpty() && !indexing) {
        Text(
            text = "Sin canciones todavía. Agrega una carpeta desde Configuración.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.uid }) { index, song ->
            key(song.uid) {
                val isCurrent = playbackState?.song?.uid == song.uid
                SwipeableSongRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && playbackState?.isPlaying == true,
                    swipeGesturesSwapped = swipeGesturesSwapped,
                    onClick = { playbackViewModel.play(songs, index) },
                    onEdit = { onEditSong(song.uid) },
                    onHide = {
                        onSetSongHidden(song.uid, true)
                        scope.launch {
                            val result =
                                snackbarHostState.showSnackbar(
                                    message = "Canción oculta",
                                    actionLabel = "Deshacer",
                                )
                            if (result == SnackbarResult.ActionPerformed) {
                                onSetSongHidden(song.uid, false)
                            }
                        }
                    },
                )
            }
        }
    }
}
