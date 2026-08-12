package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.Global

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
}