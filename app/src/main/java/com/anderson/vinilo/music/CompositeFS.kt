/*
 * Copyright (c) 2026 Vinilo Project
 * CompositeFS.kt is part of Vinilo.
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

package com.anderson.vinilo.music

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.oxycblt.musikr.fs.FS
import org.oxycblt.musikr.fs.FSUpdate
import org.oxycblt.musikr.fs.File

/**
 * Combines several [FS] sources (e.g. the device-wide MediaStore index plus user-added SAF
 * folders) into a single scan, merging their discovered files into one stream. musikr's own
 * [org.oxycblt.musikr.Config] only accepts a single [FS], and each [FS] implementation closes
 * the [Channel] it's handed once its own exploration finishes -- so each delegate is given its
 * own private channel here and pumped into the shared one, which is only closed once every
 * delegate is done.
 */
class CompositeFS(private val delegates: List<FS>) : FS {
    override suspend fun explore(files: Channel<File>): Deferred<Result<Unit>> = coroutineScope {
        try {
            val perDelegate = delegates.map { it to Channel<File>(Channel.UNLIMITED) }
            val exploreTasks = perDelegate.map { (fs, channel) -> fs.explore(channel) }
            val pumpJobs =
                perDelegate.map { (_, channel) -> launch { for (item in channel) files.send(item) } }
            exploreTasks.forEach { it.await().getOrThrow() }
            pumpJobs.forEach { it.join() }
            files.close()
            CompletableDeferred(Result.success(Unit))
        } catch (e: Throwable) {
            files.close(e)
            CompletableDeferred(Result.failure(e))
        }
    }

    override fun track(): Flow<FSUpdate> = delegates.map { it.track() }.merge()
}
