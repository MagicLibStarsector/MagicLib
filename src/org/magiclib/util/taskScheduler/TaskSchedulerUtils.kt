package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.Global
import java.util.PriorityQueue

object TaskSchedulerUtils {
    internal inline fun safeRun(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
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

            val keyed = task.key.isNotEmpty()
            val savedStates = if(keyed) SectorTaskScheduler.savedStates() else null
            queue.poll()
            // Cache before action, as action may mutate state.time (e.g. reschedule itself)
            if (keyed) savedStates?.remove(task.key)

            if (task.handle?.cancelled == true) // Canceled before its own action
                continue

            safeRun(label) { task.action.invoke() }

            val interval = task.interval ?: continue // No interval, no repeat
            if (task.handle?.cancelled == true) continue // Canceled from inside its own action

            if(keyed && savedStates != null) {
                if (savedStates.containsKey(task.key)) continue // replaced from inside its own action: drop this one
                savedStates[task.key] = task.state
            }

            var next = task.state.time + interval
            if (next <= now) next = now + interval // If we're still behind after advancing, skip the missed runs rather than firing them all back-to-back.

            task.state.time = next
            task.sequence = nextTaskSequence()

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