/*
 * Copyright (c) 2026 Vinilo Project
 * Plurals.kt is part of Vinilo.
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

/** "1 canción" vs "3 canciones" -- singular solo cuando count es exactamente 1. */
fun pluralize(count: Int, singular: String, plural: String): String =
    "$count " + if (count == 1) singular else plural
