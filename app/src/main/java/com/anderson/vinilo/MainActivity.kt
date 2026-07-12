/*
 * Copyright (c) 2026 Vinilo Project
 * MainActivity.kt is part of Vinilo.
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

package com.anderson.vinilo

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.anderson.vinilo.library.LibraryViewModel
import com.anderson.vinilo.playback.PlaybackService
import com.anderson.vinilo.ui.theme.ViniloTheme
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import org.oxycblt.musikr.Song
import org.oxycblt.musikr.covers.Cover
import org.oxycblt.musikr.tag.Name

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViniloTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LibraryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val library by viewModel.library.collectAsStateWithLifecycle()
    val indexing by viewModel.indexing.collectAsStateWithLifecycle()
    val songs = library?.songs?.sortedBy { it.name.raw }.orEmpty()

    var controller by remember { mutableStateOf<MediaController?>(null) }
    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller =
            MediaController.Builder(context, sessionToken)
                .buildAsync()
                .let { future ->
                    future.addListener({}, MoreExecutors.directExecutor())
                    future.get()
                }
    }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) viewModel.onFolderChosen(uri)
        }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { folderPicker.launch(null) }) { Text("Elegir carpeta de música") }
            if (indexing) {
                CircularProgressIndicator(modifier = Modifier.padding(start = 16.dp).size(24.dp))
            }
        }
        if (songs.isEmpty() && !indexing) {
            Text(
                text = "Sin canciones todavía. Elige una carpeta con música.",
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(songs, key = { it.uid }) { song ->
                SongRow(
                    song = song,
                    onClick = {
                        controller?.apply {
                            setMediaItem(MediaItem.fromUri(song.uri))
                            prepare()
                            play()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumbnail(cover = song.cover)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = song.name.raw, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = song.artists.joinToString { it.name.display() }.ifEmpty { "Artista desconocido" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CoverThumbnail(cover: Cover?) {
    val bitmap by
        produceState<Bitmap?>(initialValue = null, key1 = cover?.id) {
            value = cover?.open()?.use { BitmapFactory.decodeStream(it) }
        }
    Box(
        modifier =
            Modifier.size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(bitmap = loadedBitmap.asImageBitmap(), contentDescription = null)
        } else {
            Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null)
        }
    }
}

private fun Name.display(): String = (this as? Name.Known)?.raw ?: "Desconocido"
