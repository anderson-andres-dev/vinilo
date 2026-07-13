/*
 * Copyright (c) 2026 Vinilo Project
 * PlaybackViewModel.kt is part of Vinilo.
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

package com.anderson.vinilo.playback

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.anderson.vinilo.music.MusicRepository
import com.anderson.vinilo.ui.display
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.Song

data class NowPlayingUiState(
    val queue: List<Song>,
    val currentIndex: Int,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val shuffleModeEnabled: Boolean,
    val repeatMode: Int,
) {
    val song: Song
        get() = queue[currentIndex]
}

private fun nextRepeatMode(current: Int): Int =
    when (current) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    }

/**
 * Owns the single [MediaController] connection for the whole app, so playback state (and
 * control) is shared across screens rather than each one reconnecting independently. The queue is
 * just Media3's own playlist ([MediaController.setMediaItems]/[Player.seekToNext] etc.) -- [queue]
 * here is our own parallel `List<Song>` (1:1 with the controller's media item indices) so screens
 * can show title/artist/cover straight from musikr's already-resolved [Song] model instead of
 * round-tripping through [MediaMetadata].
 */
@UnstableApi
@HiltViewModel
class PlaybackViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
) : ViewModel() {
    private var controller: MediaController? = null
    private var tickerJob: Job? = null
    private val artworkCache = mutableMapOf<Music.UID, ByteArray?>()

    private val _uiState = MutableStateFlow<NowPlayingUiState?>(null)
    val uiState: StateFlow<NowPlayingUiState?> = _uiState.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            {
                val connected = future.get().also { it.addListener(playerListener) }
                controller = connected
                // The playback service can outlive this ViewModel (activity recreated, or the
                // whole process was killed and relaunched while the foreground service kept
                // playing) -- if it's already got a queue going, adopt it instead of showing no
                // mini player for a session that's actually still playing.
                if (connected.mediaItemCount > 0) reconcileWithRunningSession(connected)
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun reconcileWithRunningSession(current: MediaController) {
        viewModelScope.launch {
            val songsByUri = musicRepository.library.filterNotNull().first().songs.associateBy {
                it.uri.toString()
            }
            val queue =
                (0 until current.mediaItemCount).mapNotNull { index ->
                    songsByUri[current.getMediaItemAt(index).mediaId]
                }
            // A song under playback vanished from the library (e.g. deleted) since it started, or
            // play() already ran on this instance while we were waiting for the library -- either
            // way the running session no longer maps 1:1 onto what we can show.
            if (queue.size != current.mediaItemCount || _uiState.value != null) return@launch
            _uiState.value =
                NowPlayingUiState(
                    queue = queue,
                    currentIndex = current.currentMediaItemIndex,
                    isPlaying = current.isPlaying,
                    positionMs = current.currentPosition.coerceAtLeast(0),
                    durationMs = current.duration.coerceAtLeast(0),
                    shuffleModeEnabled = current.shuffleModeEnabled,
                    repeatMode = current.repeatMode,
                )
            playerListener.onIsPlayingChanged(current.isPlaying)
        }
    }

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                emitState(isPlaying)
                tickerJob?.cancel()
                if (isPlaying) {
                    tickerJob =
                        viewModelScope.launch {
                            while (isActive) {
                                emitState(isPlaying = true)
                                delay(500)
                            }
                        }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val current = controller ?: return
                val index = current.currentMediaItemIndex
                val state = _uiState.value ?: return
                if (index !in state.queue.indices) return
                _uiState.value = state.copy(currentIndex = index)
                // PLAYLIST_CHANGED fires from our own replaceMediaItem() call (it replaces the
                // *current* item to attach artwork) -- reacting to it here would recurse forever.
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    loadArtworkInto(index)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.value = _uiState.value?.copy(shuffleModeEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.value = _uiState.value?.copy(repeatMode = repeatMode)
            }
        }

    private fun emitState(isPlaying: Boolean) {
        val state = _uiState.value ?: return
        val current = controller ?: return
        _uiState.value =
            state.copy(
                isPlaying = isPlaying,
                positionMs = current.currentPosition.coerceAtLeast(0),
                durationMs = current.duration.coerceAtLeast(0),
            )
    }

    fun play(queue: List<Song>, startIndex: Int) {
        val current = controller ?: return
        _uiState.value =
            NowPlayingUiState(
                queue = queue,
                currentIndex = startIndex,
                isPlaying = true,
                positionMs = 0,
                durationMs = 0,
                shuffleModeEnabled = current.shuffleModeEnabled,
                repeatMode = current.repeatMode,
            )
        val mediaItems = queue.map { song -> buildMediaItem(song, artworkData = null) }
        current.setMediaItems(mediaItems, startIndex, 0)
        current.prepare()
        current.play()
        loadArtworkInto(startIndex)
    }

    private fun loadArtworkInto(index: Int) {
        val song = _uiState.value?.queue?.getOrNull(index) ?: return
        viewModelScope.launch {
            val artworkData =
                artworkCache.getOrPut(song.uid) {
                    withContext(Dispatchers.IO) { song.cover?.open()?.use { it.readBytes() } }
                }
            val current = controller ?: return@launch
            // Bail if the queue moved on while we were reading the file.
            if (_uiState.value?.queue?.getOrNull(index)?.uid != song.uid) return@launch
            current.replaceMediaItem(index, buildMediaItem(song, artworkData))
        }
    }

    private fun buildMediaItem(song: Song, artworkData: ByteArray?): MediaItem {
        val metadata =
            MediaMetadata.Builder()
                .setTitle(song.name.raw)
                .setArtist(song.artists.joinToString { it.name.display() })
                .apply {
                    if (artworkData != null) {
                        setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                }
                .build()
        return MediaItem.Builder()
            .setMediaId(song.uri.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let { it.repeatMode = nextRepeatMode(it.repeatMode) }
    }

    fun jumpToQueueIndex(index: Int) {
        controller?.seekTo(index, 0)
    }

    fun removeFromQueue(index: Int) {
        val state = _uiState.value ?: return
        if (index !in state.queue.indices) return
        val newQueue = state.queue.toMutableList().also { it.removeAt(index) }
        if (newQueue.isEmpty()) {
            stop()
            return
        }
        val removingCurrent = index == state.currentIndex
        val newCurrentIndex =
            when {
                index < state.currentIndex -> state.currentIndex - 1
                else -> state.currentIndex
            }
        _uiState.value = state.copy(queue = newQueue, currentIndex = newCurrentIndex)
        controller?.removeMediaItem(index)
        // Removing the item that's currently playing hands playback to whatever now sits at the
        // same position -- that song's MediaItem never got artwork attached, and the transition
        // listener ignores PLAYLIST_CHANGED (see onMediaItemTransition), so load it ourselves.
        if (removingCurrent) loadArtworkInto(newCurrentIndex)
    }

    fun stop() {
        tickerJob?.cancel()
        controller?.apply {
            stop()
            clearMediaItems()
        }
        _uiState.value = null
    }

    override fun onCleared() {
        tickerJob?.cancel()
        controller?.release()
        super.onCleared()
    }
}
