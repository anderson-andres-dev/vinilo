/*
 * Copyright (c) 2026 Vinilo Project
 * SettingsScreen.kt is part of Vinilo.
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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anderson.vinilo.library.LibraryTab
import com.anderson.vinilo.library.LibraryViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: LibraryViewModel = hiltViewModel()) {
    val customFolders by viewModel.customFolderUris.collectAsStateWithLifecycle(initialValue = emptySet())
    val hiddenTabs by viewModel.hiddenTabs.collectAsStateWithLifecycle(initialValue = emptySet())
    val dynamicCoverColorEnabled by
        viewModel.dynamicCoverColorEnabled.collectAsStateWithLifecycle(initialValue = true)

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) viewModel.onFolderChosen(uri)
        }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(text = "Configuración", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            text = "Biblioteca",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        SettingsActionRow(
            icon = Icons.Filled.CreateNewFolder,
            title = "Agregar carpeta",
            description = "Vinilo ya escanea toda la música del dispositivo automáticamente. " +
                "Sumá una carpeta solo si tenés música en un lugar que no se detecta solo.",
            onClick = { folderPicker.launch(null) },
        )

        if (customFolders.isNotEmpty()) {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(customFolders.toList(), key = { it.toString() }) { uri ->
                    CustomFolderRow(uri = uri, onRemove = { viewModel.onRemoveFolder(uri) })
                }
            }
        }

        Text(
            text = "Pestañas visibles",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        LibraryTab.TOGGLEABLE.forEach { tab ->
            SettingsToggleRow(
                icon = iconFor(tab),
                title = tab.title,
                description = descriptionFor(tab),
                checked = tab !in hiddenTabs,
                onCheckedChange = { checked -> viewModel.onToggleTabVisible(tab, checked) },
            )
        }

        Text(
            text = "Apariencia",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        SettingsToggleRow(
            icon = Icons.Filled.Palette,
            title = "Color dinámico desde la portada",
            description = "Adapta los colores de la app a la portada de la canción que estás escuchando.",
            checked = dynamicCoverColorEnabled,
            onCheckedChange = { checked -> viewModel.onToggleDynamicCoverColor(checked) },
        )
    }
}

private fun iconFor(tab: LibraryTab): ImageVector =
    when (tab) {
        LibraryTab.ALBUMS -> Icons.Filled.Album
        LibraryTab.ARTISTS -> Icons.Filled.Person
        LibraryTab.GENRES -> Icons.Filled.MusicNote
        LibraryTab.PLAYLISTS -> Icons.Filled.QueueMusic
        LibraryTab.SONGS -> error("Songs tab has no Settings toggle")
    }

private fun descriptionFor(tab: LibraryTab): String =
    when (tab) {
        LibraryTab.ALBUMS -> "Mostrar la pestaña Álbumes en la biblioteca."
        LibraryTab.ARTISTS -> "Mostrar la pestaña Artistas en la biblioteca."
        LibraryTab.GENRES -> "Mostrar la pestaña Géneros en la biblioteca."
        LibraryTab.PLAYLISTS -> "Mostrar la pestaña Playlists en la biblioteca."
        LibraryTab.SONGS -> error("Songs tab has no Settings toggle")
    }

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CustomFolderRow(uri: Uri, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Uri.decode(uri.lastPathSegment.orEmpty()),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Quitar carpeta")
        }
    }
}
