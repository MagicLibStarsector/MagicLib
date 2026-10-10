package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.Global
import java.util.PriorityQueue

object TaskSchedulerUtils {
    internal inline fun safeRun(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Global.getLogger(this::class.java).error(this::class.java.name + ": exception in $what", e)
        }
    }

    // Needed for easy use in java
    fun interface TaskAction {
        fun run(handle: TaskHandle)
    }

    /**
     * Runs every task in [queue] whose time has passed [now]. Requeues repeating tasks.
     */
    internal fun drainDueTasks(queue: PriorityQueue<TimedTask>, now: Long, label: String) {
        while (true) {
            val task = queue.peek() ?: break
            if (task.time >= now) break

            queue.poll()

            if (task.handle?.cancelled == true) continue

            safeRun(label) { task.action.invoke() }

            val interval = task.interval ?: continue
            if (task.handle?.cancelled == true) continue // cancelled from inside its own action

            task.time = now + interval
            queue.add(task)
        }
    }

    /**
     * Runs every callback in [tasks] once, keeping only the ones with repeat = true.
     */
    internal fun runCallbacks(tasks: MutableList<Task>, label: String) {
        if (tasks.isEmpty()) return

        val callbacks = tasks.toList()
        tasks.clear()
        callbacks.forEach {
            if (it.handle?.cancelled == true) return@forEach

            safeRun(label) { it.action.invoke() }

            if (it.repeat && it.handle?.cancelled != true) tasks.add(it)
        }
    }
}