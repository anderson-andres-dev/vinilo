/*
 * Copyright (c) 2026 Vinilo Project
 * ArtistsTab.kt is part of Vinilo.
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anderson.vinilo.music.excludingHidden
import com.anderson.vinilo.ui.CoverThumbnail
import com.anderson.vinilo.ui.display
import com.anderson.vinilo.ui.pluralize
import org.oxycblt.musikr.Artist
import org.oxycblt.musikr.Music

@Composable
fun ArtistsTab(artists: Collection<Artist>, hiddenSongs: Set<Music.UID>, onOpenArtist: (Music.UID) -> Unit) {
    val sorted = artists.sortedBy { it.name.display() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(sorted, key = { it.uid }) { artist ->
            ArtistRow(artist = artist, hiddenSongs = hiddenSongs, onClick = { onOpenArtist(artist.uid) })
        }
    }
}

@Composable
fun ArtistRow(artist: Artist, hiddenSongs: Set<Music.UID>, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumbnail(cover = artist.covers.covers.firstOrNull())
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = artist.name.display(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val albumCount = artistAlbums(artist).size
            Text(
                text =
                    "${pluralize(albumCount, "álbum", "álbumes")} · " +
                        pluralize(artist.songs.excludingHidden(hiddenSongs).size, "canción", "canciones"),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** [Artist.explicitAlbums] (album-artist tag) union [Artist.implicitAlbums] (artist tag only), deduped. */
fun artistAlbums(artist: Artist) =
    (artist.explicitAlbums + artist.implicitAlbums).distinctBy { it.uid }
