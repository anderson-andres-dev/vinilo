/*
 * Copyright (c) 2026 Vinilo Project
 * SongListItem.kt is part of Vinilo.
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

package com.anderson.vinilo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.oxycblt.musikr.Song

/**
 * The one song-row layout used everywhere a song list shows up (Biblioteca, Cola, detalle de
 * álbum/artista/género/playlist) so the "now playing" treatment stays identical across all of
 * them -- screens that need extra chrome (e.g. swipe-to-remove in the Cola) wrap this in their
 * own gesture container instead of duplicating the row layout.
 *
 * The active-row background is painted on the *unpadded* [modifier] (full row width, edge to
 * edge of whatever container this sits in) -- the 16dp horizontal margin is applied afterwards,
 * inside this composable, so it insets only the content (carátula/texto/indicador), not the
 * selection background. Callers must NOT add their own horizontal padding/contentPadding around
 * a [SongListItem] or the background would get boxed in again.
 */
@Composable
fun SongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = { CoverThumbnail(cover = song.cover, size = 48.dp) },
    showTrailingIndicator: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    if (isCurrent) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    else Color.Transparent
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = song.name.raw,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString { it.name.display() }.ifEmpty { "Artista desconocido" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent && showTrailingIndicator) {
            PlayingIndicator(isPlaying = isPlaying, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
