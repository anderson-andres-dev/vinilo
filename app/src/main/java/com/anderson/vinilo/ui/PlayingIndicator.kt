/*
 * Copyright (c) 2026 Vinilo Project
 * PlayingIndicator.kt is part of Vinilo.
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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

private data class BarSpec(val durationMs: Int, val delayMs: Int)

private val BAR_SPECS =
    listOf(BarSpec(durationMs = 950, delayMs = 0), BarSpec(durationMs = 1200, delayMs = 150), BarSpec(durationMs = 800, delayMs = 300))
private val PAUSED_HEIGHT_FRACTIONS = listOf(0.9f, 0.5f, 0.7f)
private const val MIN_HEIGHT_FRACTION = 0.25f
private val BAR_HEIGHT = 16.dp
private val BAR_WIDTH = 3.dp

/**
 * Small "now playing" equalizer: 3 bars animating up/down in a loop while [isPlaying], frozen at
 * a fixed height when paused. The single shared indicator for "this is the active song" across
 * every song list in the app (Biblioteca, Cola, detalle de álbum/artista/género/playlist) --
 * replaces ad-hoc row highlighting.
 */
@Composable
fun PlayingIndicator(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "playingIndicator")
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.size(width = BAR_WIDTH * 3 + 4.dp, height = BAR_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BAR_SPECS.forEachIndexed { index, spec ->
            val heightFraction =
                if (isPlaying) {
                    transition.animateFloat(
                        initialValue = MIN_HEIGHT_FRACTION,
                        targetValue = 1f,
                        animationSpec =
                            infiniteRepeatable(
                                animation =
                                    tween(
                                        durationMillis = spec.durationMs,
                                        delayMillis = spec.delayMs,
                                        easing = LinearEasing,
                                    ),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        label = "bar$index",
                    ).value
                } else {
                    PAUSED_HEIGHT_FRACTIONS[index]
                }
            Canvas(modifier = Modifier.size(width = BAR_WIDTH, height = BAR_HEIGHT)) {
                val barHeight = size.height * heightFraction
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                    size = Size(size.width, barHeight),
                )
            }
        }
    }
}
