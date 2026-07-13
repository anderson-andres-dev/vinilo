/*
 * Copyright (c) 2026 Vinilo Project
 * LibraryViewModel.kt is part of Vinilo.
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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anderson.vinilo.music.MusicRepository
import com.anderson.vinilo.settings.LibrarySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.oxycblt.musikr.Music

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val musicRepository: MusicRepository,
    private val librarySettings: LibrarySettingsRepository,
) : ViewModel() {
    val library = musicRepository.library
    val indexing = musicRepository.indexing
    val customFolderUris = musicRepository.customFolderUris
    val hiddenTabs = librarySettings.hiddenTabs
    val dynamicCoverColorEnabled = librarySettings.dynamicCoverColorEnabled
    val hiddenSongs = librarySettings.hiddenSongs
    val swipeGesturesSwapped = librarySettings.swipeGesturesSwapped

    fun onAudioPermissionGranted() {
        viewModelScope.launch { musicRepository.rescan() }
    }

    fun onFolderChosen(uri: Uri) {
        viewModelScope.launch { musicRepository.addCustomFolder(uri) }
    }

    fun onRemoveFolder(uri: Uri) {
        viewModelScope.launch { musicRepository.removeCustomFolder(uri) }
    }

    fun onToggleTabVisible(tab: LibraryTab, visible: Boolean) {
        viewModelScope.launch { librarySettings.setTabHidden(tab, hidden = !visible) }
    }

    fun onToggleDynamicCoverColor(enabled: Boolean) {
        viewModelScope.launch { librarySettings.setDynamicCoverColorEnabled(enabled) }
    }

    fun onSetSongHidden(uid: Music.UID, hidden: Boolean) {
        viewModelScope.launch { librarySettings.setSongHidden(uid, hidden) }
    }

    fun onToggleSwipeGesturesSwapped(swapped: Boolean) {
        viewModelScope.launch { librarySettings.setSwipeGesturesSwapped(swapped) }
    }
}
