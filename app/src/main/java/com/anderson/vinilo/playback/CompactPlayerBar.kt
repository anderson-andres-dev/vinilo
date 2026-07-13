/*
 * Copyright (c) 2026 Vinilo Project
 * CompactPlayerBar.kt is part of Vinilo.
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anderson.vinilo.ui.CoverThumbnail
import com.anderson.vinilo.ui.display

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactPlayerBar(viewModel: PlaybackViewModel, onOpenNowPlaying: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val current = state ?: return
    val haptics = LocalHapticFeedback.current

    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) viewModel.stop()
                true
            }
        )

    SwipeToDismissBox(state = dismissState, backgroundContent = {}) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                val progress =
                    if (current.durationMs > 0) {
                        (current.positionMs.toFloat() / current.durationMs).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .clickable(onClick = onOpenNowPlaying),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverThumbnail(cover = current.song.cover, size = 40.dp)
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(
                            text = current.song.name.raw,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                        Text(
                            text =
                                current.song.artists.joinToString { it.name.display() }
                                    .ifEmpty { "Artista desconocido" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            viewModel.playPause()
                        }
                    ) {
                        Icon(
                            imageVector = if (current.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (current.isPlaying) "Pausar" else "Reproducir",
                        )
                    }
                    IconButton(onClick = { viewModel.next() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Siguiente",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
