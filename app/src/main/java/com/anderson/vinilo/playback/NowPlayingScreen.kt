/*
 * Copyright (c) 2026 Vinilo Project
 * NowPlayingScreen.kt is part of Vinilo.
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

package com.anderson.vinilo.playback

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.anderson.vinilo.ui.CoverThumbnail
import com.anderson.vinilo.ui.display
import com.anderson.vinilo.ui.formatDuration
import com.anderson.vinilo.ui.pluralize

@Composable
fun NowPlayingScreen(viewModel: PlaybackViewModel, onBack: () -> Unit, onOpenQueue: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state == null) onBack()
    }
    val current = state ?: return

    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }
    val displayedPositionMs = if (isSeeking) seekPositionMs.toLong() else current.positionMs

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Cerrar")
            }
            Text(
                text = "Cola · ${pluralize(current.queue.size, "canción", "canciones")}",
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = onOpenQueue) {
                Icon(imageVector = Icons.Filled.QueueMusic, contentDescription = "Cola")
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            CoverThumbnail(cover = current.song.cover, size = 280.dp, cornerRadius = 20.dp)
        }

        Text(
            text = current.song.name.raw,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
        )
        Text(
            text =
                current.song.artists.joinToString { it.name.display() }
                    .ifEmpty { "Artista desconocido" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )

        Slider(
            value = displayedPositionMs.toFloat(),
            valueRange = 0f..current.durationMs.coerceAtLeast(1).toFloat(),
            onValueChange = {
                isSeeking = true
                seekPositionMs = it
            },
            onValueChangeFinished = {
                viewModel.seekTo(seekPositionMs.toLong())
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = displayedPositionMs.formatDuration(), style = MaterialTheme.typography.bodySmall)
            Text(text = current.durationMs.formatDuration(), style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = { viewModel.cycleRepeatMode() }) {
                Icon(
                    imageVector =
                        if (current.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Filled.RepeatOne
                        } else {
                            Icons.Filled.Repeat
                        },
                    contentDescription = "Repetir",
                    tint =
                        if (current.repeatMode != Player.REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            IconButton(onClick = { viewModel.previous() }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Anterior",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = { viewModel.playPause() }, modifier = Modifier.size(72.dp)) {
                Icon(
                    imageVector = if (current.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (current.isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(64.dp),
                )
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Siguiente",
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledTonalIconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Aleatorio",
                    tint =
                        if (current.shuffleModeEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
