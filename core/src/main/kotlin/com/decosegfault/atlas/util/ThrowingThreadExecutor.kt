/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas.util

import org.tinylog.kotlin.Logger
import java.util.concurrent.*


/**
 * Modification to [ThreadPoolExecutor] that throws exceptions if they occur
 *
 * @author Matt Young
 */
class ThrowingThreadExecutor(
    coreSize: Int, maxSize: Int, time: Long, timeUnit: TimeUnit, queue: BlockingQueue<Runnable>,
    threadFactory: ThreadFactory
) : ThreadPoolExecutor(coreSize, maxSize, time, timeUnit, queue, threadFactory) {

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
        var newT = t
        if (t == null && r is Future<*>) {
            try {
                if (r.isDone) {
                    r.get()
                }
            } catch (ce: CancellationException) {
                newT = ce
            } catch (ee: ExecutionException) {
                newT = ee.cause
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        if (newT != null) {
            Logger.error("Exception in worker: $newT")
            Logger.error(newT)
        }
    }

}
