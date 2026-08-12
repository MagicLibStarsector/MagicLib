package org.magiclib.util.taskScheduler

data class TimedTask(
    val action: () -> Unit,          // The deferred logic to run when this task comes due.
    var time: Long,                  // Scheduled-run time
    val interval: Long? = null,      // Repeat period in ms; null means run once, non-null reschedules after each run.
    val handle: TaskHandle? = null   // Cancellation token checked before each run; a cancelled task is dropped instead of executed.
) : Comparable<TimedTask> {
    override fun compareTo(other: TimedTask): Int = time.compareTo(other.time)
}