package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.Global

internal object TaskSchedulerUtils {
    inline fun safeRun(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Global.getLogger(this::class.java).error(this::class.java.name + ": exception in $what", e)
        }
    }
}