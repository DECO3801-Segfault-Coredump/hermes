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
