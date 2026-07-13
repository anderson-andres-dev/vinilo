/*
 * Copyright (c) 2026 Vinilo Project
 * LibraryScreen.kt is part of Vinilo.
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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anderson.vinilo.playback.PlaybackViewModel
import org.oxycblt.musikr.Music

private val AUDIO_PERMISSION =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private val TAB_TITLES = listOf("Canciones", "Álbumes", "Artistas", "Géneros", "Playlists")

@Composable
fun LibraryScreen(
    playbackViewModel: PlaybackViewModel,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbum: (Music.UID) -> Unit,
    onOpenArtist: (Music.UID) -> Unit,
    onOpenGenre: (Music.UID) -> Unit,
    onOpenPlaylist: (Music.UID) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val library by viewModel.library.collectAsStateWithLifecycle()
    val indexing by viewModel.indexing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, AUDIO_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasAudioPermission = granted
        }
    LaunchedEffect(Unit) {
        if (!hasAudioPermission) permissionLauncher.launch(AUDIO_PERMISSION)
    }
    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) viewModel.onAudioPermissionGranted()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Vinilo", style = MaterialTheme.typography.titleLarge)
                if (indexing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = 12.dp).size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSearch) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Buscar")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "Configuración")
                }
            }
        }
        if (!hasAudioPermission) {
            Text(
                text = "Vinilo necesita permiso para acceder a tu música.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
            return@Column
        }

        PrimaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            TAB_TITLES.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTab) {
            0 ->
                SongsTab(
                    songs = library?.songs?.sortedBy { it.name.raw }.orEmpty(),
                    indexing = indexing,
                    playbackViewModel = playbackViewModel,
                )
            1 -> AlbumsTab(albums = library?.albums.orEmpty(), onOpenAlbum = onOpenAlbum)
            2 -> ArtistsTab(artists = library?.artists.orEmpty(), onOpenArtist = onOpenArtist)
            3 -> GenresTab(genres = library?.genres.orEmpty(), onOpenGenre = onOpenGenre)
            4 -> PlaylistsTab(playlists = library?.playlists.orEmpty(), onOpenPlaylist = onOpenPlaylist)
        }
    }
}
