/*
 * Copyright (c) 2026 Vinilo Project
 * PlaybackService.kt is part of Vinilo.
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

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Minimal playback backend: one [ExoPlayer] behind one [MediaSession]. No custom notification or
 * persistence of playback state yet.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, AlwaysSkippablePlayer(player)).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.let {
            it.player.release()
            it.release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

/**
 * ExoPlayer reports seek-to-next/previous as unavailable at the edges of the queue (no next item,
 * repeat off) -- Media3's system notification and lock-screen controls react by *removing* that
 * action entirely instead of graying it out, so the button layout shifts between tracks. This
 * forces both commands to always report available so the notification/lock screen always shows
 * all three controls in the same position; actually tapping next/previous at the edge is already
 * a safe no-op in Media3's own seekToNext()/seekToPrevious() (stays put, or restarts the current
 * track), which reads to the user as "disabled" without Android's media-session API actually
 * supporting a disabled-but-visible state.
 */
private class AlwaysSkippablePlayer(player: Player) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands()
            .buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()

    override fun isCommandAvailable(command: Int): Boolean =
        when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
            else -> super.isCommandAvailable(command)
        }
}
