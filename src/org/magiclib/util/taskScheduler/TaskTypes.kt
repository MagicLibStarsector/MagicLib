package org.magiclib.util.taskScheduler

import java.util.concurrent.atomic.AtomicLong

class TaskHandle {
    var cancelled = false; private set
    fun cancel() {
        cancelled = true
    }
}

// Saved with the game.
class TaskState(
    val key: String = "",
    var time: Long,                 // Scheduled-run time
    val interval: Long? = null,     // Repeat period in ms; null means run once, non-null reschedules after each run.
) {
    init {
        require(interval == null || interval >= 0L) { "Task '$key': interval must either be null or non-negative when set, but was $interval ms" }
    }
}

private val sequenceCounter = AtomicLong()
internal fun nextTaskSequence(): Long = sequenceCounter.getAndIncrement()

class TimedTask(
    val action: () -> Unit,         // The deferred logic to run when this task comes due.
    val state: TaskState,
    val handle: TaskHandle? = null  // Cancellation token checked before each run; a cancelled task is dropped instead of executed.
) : Comparable<TimedTask> {
    val time get() = state.time
    val interval get() = state.interval
    val key get() = state.key

    /** Tiebreaker for tasks scheduled at the same time. Lower runs first. */
    internal var sequence: Long = nextTaskSequence()

    override fun compareTo(other: TimedTask): Int {
        val byTime = time.compareTo(other.time)
        if (byTime != 0) return byTime // Times differ, earlier time goes first
        return sequence.compareTo(other.sequence) // Times same, lower sequence goes first
    }

    //override fun equals(other: Any?) = other is TimedTask && other.key == key && other.time == time && other.interval == interval
    //override fun hashCode() = key.hashCode()
}

data class Task(
    val action: () -> Unit,          // The deferred logic to run when this task comes due.
    val repeat: Boolean = false,     // If true, the task will be rescheduled after execution.
    val handle: TaskHandle? = null   // Cancellation token checked before each run; a canceled task is dropped instead of executed.
)