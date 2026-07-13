/*
 * Copyright (c) 2026 Vinilo Project
 * CoverAccentColors.kt is part of Vinilo.
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

package com.anderson.vinilo.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

data class CoverAccentColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
)

private fun Palette.Swatch.onColor(): Color = if (hsl[2] > 0.5f) Color.Black else Color.White

/** CPU-bound -- call from Dispatchers.Default. Null if no data or no usable swatch. */
fun extractCoverAccentColors(artworkBytes: ByteArray): CoverAccentColors? =
    runCatching {
            val bitmap = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.size) ?: return null
            val palette = Palette.from(bitmap).generate()

            val primarySwatch =
                palette.vibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                    ?: return null
            val primaryContainerSwatch =
                palette.lightVibrantSwatch ?: palette.lightMutedSwatch ?: primarySwatch
            val secondarySwatch =
                palette.mutedSwatch?.takeIf { it != primarySwatch }
                    ?: palette.darkMutedSwatch
                    ?: primarySwatch
            val tertiarySwatch =
                palette.darkVibrantSwatch?.takeIf { it != primarySwatch }
                    ?: palette.lightMutedSwatch?.takeIf { it != primarySwatch }
                    ?: secondarySwatch

            CoverAccentColors(
                primary = Color(primarySwatch.rgb),
                onPrimary = primarySwatch.onColor(),
                primaryContainer = Color(primaryContainerSwatch.rgb),
                onPrimaryContainer = primaryContainerSwatch.onColor(),
                secondary = Color(secondarySwatch.rgb),
                onSecondary = secondarySwatch.onColor(),
                tertiary = Color(tertiarySwatch.rgb),
                onTertiary = tertiarySwatch.onColor(),
            )
        }
        .getOrNull()
