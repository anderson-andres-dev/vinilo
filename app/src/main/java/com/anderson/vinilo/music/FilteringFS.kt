/*
 * Copyright (c) 2026 Vinilo Project
 * FilteringFS.kt is part of Vinilo.
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
import kotlinx.coroutines.launch
import org.oxycblt.musikr.fs.FS
import org.oxycblt.musikr.fs.FSUpdate
import org.oxycblt.musikr.fs.File

/**
 * Wraps an [FS], forwarding only the [File]s that match [predicate]. Same channel-ownership
 * contract as [CompositeFS]: the delegate is given its own private channel (since it closes
 * whatever channel it's handed once its own exploration finishes), and the shared [files]
 * channel passed to [explore] is only closed once the delegate is fully done.
 */
class FilteringFS(private val delegate: FS, private val predicate: (File) -> Boolean) : FS {
    override suspend fun explore(files: Channel<File>): Deferred<Result<Unit>> = coroutineScope {
        try {
            val delegateChannel = Channel<File>(Channel.UNLIMITED)
            val exploreTask = delegate.explore(delegateChannel)
            val pumpJob = launch { for (item in delegateChannel) if (predicate(item)) files.send(item) }
            exploreTask.await().getOrThrow()
            pumpJob.join()
            files.close()
            CompletableDeferred(Result.success(Unit))
        } catch (e: Throwable) {
            files.close(e)
            CompletableDeferred(Result.failure(e))
        }
    }

    override fun track(): Flow<FSUpdate> = delegate.track()
}
