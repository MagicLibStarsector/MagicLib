package org.magiclib.util.taskScheduler

import org.magiclib.util.taskScheduler.TaskSchedulerUtils.drainDueTasks
import java.util.*

/**
 * A task scheduler for timed, recurring, and event-driven actions in the current session.
 *
 * Example Java Usage:
 * ```
 * SystemTaskScheduler.performLater(0L, handle -> {
 *     Global.getLogger(this.getClass()).warn("TEST");
 * });
 * ```
 * Example Kotlin Usage:
 * ```
 * SystemTaskScheduler.performLater(0L) { handle ->
 *     Global.getLogger(this::class.java).warn("TEST")
 * }
 * ```
 *
 * @author S-Numan
 */
object SystemTaskScheduler {

    @JvmStatic
    fun getSystemTasks(): Collection<TimedTask> {
        return systemTimeQueue.toList()
    }

    /**
     * Runs [action] once, [delay] milliseconds from now.
     *
     * Timing follows [System.nanoTime] in milliseconds.
     *
     * Persists in the current session, does not persist through game restart.
     */
    @JvmStatic
    @JvmOverloads
    fun performLater(delay: Long = 0, action: TaskSchedulerUtils.TaskAction): TaskHandle {
        val handle = TaskHandle()

        val task = TimedTask(
            action = { action.run(handle) },
            time = System.nanoTime() + delay * 1_000_000L,
            interval = null,
            handle = handle
        )

        systemTimeQueue.add(task)

        return handle
    }

    /**
     * Runs [action] every [interval] milliseconds, starting after one interval.
     *
     * Timing follows [System.nanoTime] in milliseconds.
     *
     * Persists in the current session, does not persist through game restart.
     */
    @JvmStatic
    fun performEvery(interval: Long = 0, action: TaskSchedulerUtils.TaskAction): TaskHandle {
        val handle = TaskHandle()

        val convertedInterval = interval * 1_000_000L

        val task = TimedTask(
            action = { action.run(handle) }, // inject handle into lambda
            time = System.nanoTime() + convertedInterval,
            interval = convertedInterval,
            handle = handle
        )

        systemTimeQueue.add(task)

        return handle
    }

    private val systemTimeQueue = PriorityQueue<TimedTask>()

    internal fun internalAdvance() {
        if(systemTimeQueue.isNotEmpty())
            drainDueTasks(systemTimeQueue, System.nanoTime(), "deferred systemTime task")
    }
}