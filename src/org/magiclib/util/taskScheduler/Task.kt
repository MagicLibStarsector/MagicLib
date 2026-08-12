package org.magiclib.util.taskScheduler

data class Task(
    val action: () -> Unit,          // The deferred logic to run when this task comes due.
    val repeat: Boolean = false,     // If true, the task will be rescheduled after execution.
    val handle: TaskHandle? = null   // Cancellation token checked before each run; a canceled task is dropped instead of executed.
)