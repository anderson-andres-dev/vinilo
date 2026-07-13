/*
 * Copyright (c) 2026 Vinilo Project
 * SwipeableSongRow.kt is part of Vinilo.
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.oxycblt.musikr.Song

/**
 * Wraps [SongListItem] with swipe-to-reveal gestures for two safe, reversible actions -- Editar
 * and Quitar. Destructive actions (Eliminar, still unbuilt) deliberately never live behind a
 * single swipe -- that's reserved for a future long-press multi-select flow instead, so this
 * composable only ever needs two directions/two outcomes.
 *
 * Editar keeps the row mounted (the song stays in the list) so its swipe direction vetoes the
 * dismiss (`confirmValueChange` returns `false`), which -- per Material3's own
 * `AnchoredDraggableState.settle()` -- animates the row back to [SwipeToDismissBoxValue.Settled]
 * automatically. Quitar removes the song from whatever list is showing it, so its direction
 * confirms the dismiss (`true`) exactly like the existing swipe-to-remove pattern in
 * `playback/QueueScreen.kt`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    swipeGesturesSwapped: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editDirection =
        if (swipeGesturesSwapped) SwipeToDismissBoxValue.EndToStart else SwipeToDismissBoxValue.StartToEnd
    // rememberSwipeToDismissBoxState only builds its confirmValueChange closure once, so reading
    // editDirection through rememberUpdatedState keeps it honoring live changes to the Settings
    // gesture-swap toggle instead of freezing whatever the mapping was on first composition.
    val currentEditDirection by rememberUpdatedState(editDirection)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnHide by rememberUpdatedState(onHide)
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    currentEditDirection -> {
                        currentOnEdit()
                        false
                    }
                    SwipeToDismissBoxValue.Settled -> true
                    else -> {
                        currentOnHide()
                        true
                    }
                }
            }
        )
    // Defensive reset: a song that reappears after being un-hidden must never start already
    // visually swiped-away, even if the LazyColumn slot for its key still held the previous
    // dismissed state from before it left the list.
    LaunchedEffect(song.uid) { dismissState.reset() }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = { SwipeActionBackground(dismissState = dismissState, editDirection = editDirection) },
    ) {
        SongListItem(
            song = song,
            isCurrent = isCurrent,
            isPlaying = isPlaying,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActionBackground(dismissState: SwipeToDismissBoxState, editDirection: SwipeToDismissBoxValue) {
    val direction = dismissState.dismissDirection
    if (direction == SwipeToDismissBoxValue.Settled) return

    val isEdit = direction == editDirection
    val icon: ImageVector = if (isEdit) Icons.Filled.Edit else Icons.Filled.VisibilityOff
    val containerColor =
        if (isEdit) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val contentColor =
        if (isEdit) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    val progress = dismissState.progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxSize().background(containerColor).padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isEdit) "Editar" else "Quitar",
            tint = contentColor.copy(alpha = progress),
            modifier = Modifier.scale(0.6f + 0.4f * progress),
        )
    }
}
