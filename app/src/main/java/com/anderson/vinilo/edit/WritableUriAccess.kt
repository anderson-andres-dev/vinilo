/*
 * Copyright (c) 2026 Vinilo Project
 * WritableUriAccess.kt is part of Vinilo.
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

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface WriteAccessResult {
    data object Granted : WriteAccessResult

    data class NeedsConsent(val intentSender: IntentSender) : WriteAccessResult

    data object UnsupportedOnThisApiLevel : WriteAccessResult
}

fun isMediaStoreUri(uri: Uri): Boolean = uri.authority == MediaStore.AUTHORITY

/**
 * Resolves write access for [uri], source-agnostic: SAF custom-folder songs already carry a
 * persisted write grant (see `Location.Unopened.open()` in musikr's `fs/Location.kt`) so this
 * returns [WriteAccessResult.Granted] immediately with no dialog; MediaStore-sourced songs need a
 * one-time system consent dialog on API 29+ (not supported below that, see
 * [WriteAccessResult.UnsupportedOnThisApiLevel]).
 */
suspend fun requestWriteAccess(context: Context, uri: Uri): WriteAccessResult =
    withContext(Dispatchers.IO) {
        if (isMediaStoreUri(uri) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext WriteAccessResult.UnsupportedOnThisApiLevel
        }
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use {}
            WriteAccessResult.Granted
        } catch (e: SecurityException) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    WriteAccessResult.NeedsConsent(
                        MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender
                    )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException ->
                    WriteAccessResult.NeedsConsent(e.userAction.actionIntent.intentSender)
                else -> throw e
            }
        }
    }
