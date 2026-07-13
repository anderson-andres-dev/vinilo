/*
 * Copyright (c) 2026 Vinilo Project
 * EditSongViewModel.kt is part of Vinilo.
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

import android.content.Context
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anderson.vinilo.music.MusicRepository
import com.anderson.vinilo.music.tagediting.EditableSongTags
import com.anderson.vinilo.music.tagediting.orFallback
import com.anderson.vinilo.music.tagediting.readSongTags
import com.anderson.vinilo.music.tagediting.writeSongTags
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.oxycblt.musikr.Song

@HiltViewModel
class EditSongViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState

        data class Editing(val tags: EditableSongTags, val error: String? = null) : UiState

        data object Saving : UiState

        data class NeedsConsent(val intentSender: IntentSender, val pendingTags: EditableSongTags) : UiState

        data object UnsupportedApiLevel : UiState

        data object Saved : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    val indexing = musicRepository.indexing

    fun load(song: Song) {
        if (isMediaStoreUri(song.uri) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _state.value = UiState.UnsupportedApiLevel
            return
        }
        viewModelScope.launch {
            val tags = readSongTags(context, song.uri).orFallback(song)
            _state.value = UiState.Editing(tags)
        }
    }

    fun onSaveClicked(song: Song, tags: EditableSongTags) {
        viewModelScope.launch {
            _state.value = UiState.Saving
            when (val access = requestWriteAccess(context, song.uri)) {
                WriteAccessResult.Granted -> commitWrite(song, tags)
                is WriteAccessResult.NeedsConsent ->
                    _state.value = UiState.NeedsConsent(access.intentSender, tags)
                WriteAccessResult.UnsupportedOnThisApiLevel -> _state.value = UiState.UnsupportedApiLevel
            }
        }
    }

    fun onConsentResult(granted: Boolean, song: Song, tags: EditableSongTags) {
        if (!granted) {
            _state.value = UiState.Editing(tags, error = "Permiso denegado.")
            return
        }
        viewModelScope.launch { commitWrite(song, tags) }
    }

    private suspend fun commitWrite(song: Song, tags: EditableSongTags) {
        val result = runCatching { writeSongTags(context, song.uri, tags) }
        if (result.getOrDefault(false)) {
            musicRepository.rescan()
            _state.value = UiState.Saved
        } else {
            val detail = result.exceptionOrNull()?.message
            _state.value =
                UiState.Editing(
                    tags,
                    error = "No se pudo guardar los cambios." + (detail?.let { " ($it)" } ?: ""),
                )
        }
    }
}
