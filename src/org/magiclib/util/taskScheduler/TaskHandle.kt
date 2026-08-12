package org.magiclib.util.taskScheduler

class TaskHandle {
    var cancelled = false
    fun cancel() {
        cancelled = true
    }
}