/*
 * Copyright (c) 2026 Vinilo Project
 * LibrarySettingsRepository.kt is part of Vinilo.
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

package com.anderson.vinilo.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anderson.vinilo.library.LibraryTab
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "library_settings")

/**
 * Persists the extra SAF folders the user has manually added on top of the device-wide
 * MediaStore-backed default library (see [com.anderson.vinilo.music.MusicRepository]).
 */
@Singleton
class LibrarySettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val customFolderUrisKey = stringSetPreferencesKey("custom_folder_uris")

    val customFolderUris: Flow<Set<Uri>> =
        context.libraryDataStore.data.map { prefs ->
            prefs[customFolderUrisKey].orEmpty().map(Uri::parse).toSet()
        }

    suspend fun addCustomFolder(uri: Uri) {
        context.libraryDataStore.edit { prefs ->
            prefs[customFolderUrisKey] = prefs[customFolderUrisKey].orEmpty() + uri.toString()
        }
    }

    suspend fun removeCustomFolder(uri: Uri) {
        context.libraryDataStore.edit { prefs ->
            prefs[customFolderUrisKey] = prefs[customFolderUrisKey].orEmpty() - uri.toString()
        }
    }

    private val hiddenTabTypesKey = stringSetPreferencesKey("hidden_tab_types")

    val hiddenTabs: Flow<Set<LibraryTab>> =
        context.libraryDataStore.data.map { prefs ->
            prefs[hiddenTabTypesKey].orEmpty()
                .mapNotNull { name -> runCatching { LibraryTab.valueOf(name) }.getOrNull() }
                .toSet() - LibraryTab.SONGS
        }

    suspend fun setTabHidden(tab: LibraryTab, hidden: Boolean) {
        if (tab == LibraryTab.SONGS) return
        context.libraryDataStore.edit { prefs ->
            val current = prefs[hiddenTabTypesKey].orEmpty()
            prefs[hiddenTabTypesKey] = if (hidden) current + tab.name else current - tab.name
        }
    }
}
