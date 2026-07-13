/*
 * Copyright (c) 2026 Vinilo Project
 * EditSongScreen.kt is part of Vinilo.
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

package com.anderson.vinilo.edit

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anderson.vinilo.library.LibraryViewModel
import com.anderson.vinilo.music.tagediting.EditableSongTags
import com.anderson.vinilo.music.tagediting.joinForDisplay
import com.anderson.vinilo.music.tagediting.splitToTagValues
import org.oxycblt.musikr.Music

@Composable
fun EditSongScreen(
    uid: Music.UID,
    onBack: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    viewModel: EditSongViewModel = hiltViewModel(),
) {
    val library by libraryViewModel.library.collectAsStateWithLifecycle()
    val song = library?.findSong(uid)
    LaunchedEffect(library) {
        if (library != null && song == null) onBack()
    }
    if (song == null) return

    val state by viewModel.state.collectAsStateWithLifecycle()
    val indexing by viewModel.indexing.collectAsStateWithLifecycle()

    LaunchedEffect(song.uid) { viewModel.load(song) }

    LaunchedEffect(state) {
        if (state is EditSongViewModel.UiState.Saved) onBack()
    }

    val consentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val needsConsent = state as? EditSongViewModel.UiState.NeedsConsent
            if (needsConsent != null) {
                viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK, song, needsConsent.pendingTags)
            }
        }
    LaunchedEffect(state) {
        val needsConsent = state as? EditSongViewModel.UiState.NeedsConsent
        if (needsConsent != null) {
            consentLauncher.launch(IntentSenderRequest.Builder(needsConsent.intentSender).build())
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(text = "Editar canción", style = MaterialTheme.typography.titleLarge)
        }

        when (val current = state) {
            EditSongViewModel.UiState.Loading -> {}
            EditSongViewModel.UiState.UnsupportedApiLevel ->
                Text(
                    text = "No disponible en Android <10 para esta canción.",
                    modifier = Modifier.padding(top = 24.dp),
                )
            EditSongViewModel.UiState.Saved -> {}
            else -> {
                val tags =
                    when (current) {
                        is EditSongViewModel.UiState.Editing -> current.tags
                        is EditSongViewModel.UiState.NeedsConsent -> current.pendingTags
                        else -> null
                    }
                if (tags != null) {
                    var title by remember(song.uid) { mutableStateOf(tags.title) }
                    var artists by remember(song.uid) { mutableStateOf(tags.artists.joinForDisplay()) }
                    var genres by remember(song.uid) { mutableStateOf(tags.genres.joinForDisplay()) }
                    val saving =
                        current is EditSongViewModel.UiState.Saving ||
                            current is EditSongViewModel.UiState.NeedsConsent ||
                            indexing

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                    OutlinedTextField(
                        value = artists,
                        onValueChange = { artists = it },
                        label = { Text("Artista(s)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    OutlinedTextField(
                        value = genres,
                        onValueChange = { genres = it },
                        label = { Text("Género(s)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )

                    if (current is EditSongViewModel.UiState.Editing && current.error != null) {
                        Text(
                            text = current.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.onSaveClicked(
                                song,
                                EditableSongTags(title, artists.splitToTagValues(), genres.splitToTagValues()),
                            )
                        },
                        enabled = !saving,
                        modifier = Modifier.padding(top = 24.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}
