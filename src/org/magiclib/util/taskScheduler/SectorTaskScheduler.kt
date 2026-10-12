package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignClockAPI
import org.magiclib.util.taskScheduler.TaskSchedulerUtils.drainDueTasks
import org.magiclib.util.taskScheduler.TaskSchedulerUtils.runCallbacks
import java.util.*

/**
 * A task scheduler for timed, recurring, and event-driven actions in the sector.
 *
 * Example Java Usage:
 * ```
 * SectorTaskScheduler.performLater(0L, "", handle -> {
 *     Global.getLogger(this.getClass()).warn("TEST");
 * });
 * ```
 * Example Kotlin Usage:
 * ```
 * SectorTaskScheduler.performLater(0L) { handle ->
 *     Global.getLogger(this::class.java).warn("TEST")
 * }
 * ```
 *
 * @author S-Numan
 */
class SectorTaskScheduler : EveryFrameScript {
    init {
        CombatTaskScheduler.onBattleStart.clear()
    }

    companion object {
        private var active: SectorTaskScheduler? = null
        internal fun setActive(value: SectorTaskScheduler?) {
            active = value
        }
        private fun getActiveOrNull(): SectorTaskScheduler? {
            if(active == null) {
                val caller = Throwable().stackTrace.getOrNull(1)
                val callerName = caller?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" } ?: "unknown"
                Global.getLogger(this::class.java).warn("Function [$callerName] called when not in sector. Cannot add task.")
            }
            return active
        }

        private const val PERSIST_KEY = "ML_persistentTaskTimers"

        @Suppress("UNCHECKED_CAST")
        internal fun savedStates(): MutableMap<String, TaskState> =
            Global.getSector().persistentData.getOrPut(PERSIST_KEY) { HashMap<String, TaskState>() } as MutableMap<String, TaskState>


        private fun resolveState(key: String, defaultTime: Long, interval: Long?): TaskState {
            if (key.isEmpty()) return TaskState("", defaultTime, interval) // unkeyed: not persisted

            val saved = savedStates()
            val existing = saved[key]
            if (existing != null && existing.interval == interval) return existing
            return TaskState(key, defaultTime, interval).also { saved[key] = it }
        }

        @JvmStatic
        fun getSectorTasks(): Collection<TimedTask> {
            return active?.sectorTimeQueue?.sorted() ?: emptyList()
        }

        /** Avoid having an action call this on itself, use [TaskHandle.cancel] if possible instead. */
        @JvmStatic
        fun removeSectorTaskWithKey(key: String) {
            if(key.isEmpty()) return

            active?.sectorTimeQueue?.removeAll { it.state.key == key }
            savedStates().remove(key)
        }

        @JvmStatic
        fun getOnUnpauseTasks(): Collection<Task> {
            return active?.onUnpause?.toList() ?: emptyList()
        }

        @JvmStatic
        fun getAfterPlayerBattleTasks(): Collection<Task> {
            return active?.afterPlayerBattle?.toList() ?: emptyList()
        }

        @JvmStatic
        fun getAfterSectorExitTasks(): Collection<Task> {
            return onSectorExit.toList()
        }

        /**
         * [CampaignClockAPI.getTimestamp] advances in fixed calendar milliseconds (86,400,000 per in-game day) regardless of how fast that day actually passes in real time.
         *
         * [CampaignClockAPI.getSecondsPerDay] gives the real-seconds-per-game-day ratio so this converts a duration in real milliseconds, at normal/unaccelerated game speed, into the matching calendar-millisecond delta.
         */
        private fun realMsToClockMs(realMs: Long): Long {
            val secondsPerDay = Global.getSector().clock.secondsPerDay.toDouble()
            return (realMs * (86_400.0 / secondsPerDay)).toLong()
        }


        /**
         * Runs [action] once, [delay] milliseconds from now.
         *
         * Timing follows the sector clock and stops advancing while the game is paused. Elapsed real time will differ from [delay] if the game's time acceleration
         * (e.g. fast-forward) is active, or if a mod changes [CampaignClockAPI.getSecondsPerDay].
         *
         * - The task does not persist in the save file, re-register the task after every [BaseModPlugin.onGameLoad] as needed.
         * - Provide a unique [key] to persist the task's timer: Re-registering the task with the same [key] resumes its existing schedule rather than restarting the timer, provided the task was left unfinished.
         * - If a task is overdue, it runs once immediately, then resumes its regular schedule.
         *
         * @param key Unique ID for persisting the timer. Leave empty for a non-persistent task.
         * @return A [TaskHandle] that can be used to cancel the task.
         */
        @JvmStatic
        fun performLater(delay: Long = 0, key: String = "", action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = getActiveOrNull() ?: return handle

            val state = resolveState(key, Global.getSector().clock.timestamp + realMsToClockMs(delay), null)

            if(key.isNotEmpty()) inst.sectorTimeQueue.removeAll { it.key == key }
            inst.sectorTimeQueue.add(
                TimedTask({ action.run(handle) }, // inject handle into lambda
                    state,
                    handle)
            )

            return handle
        }

        /**
         * Runs [action] every [interval] milliseconds, starting after one interval.
         *
         * Timing follows the sector clock and stops advancing while the game is paused. Elapsed real time will differ from [interval] if the game's time acceleration
         * (e.g. fast-forward) is active, or if a mod changes [CampaignClockAPI.getSecondsPerDay].
         *
         * - The task does not persist in the save file, re-register the task after every [BaseModPlugin.onGameLoad] as needed.
         * - Provide a unique [key] to persist the task's timer: Re-registering the task with the same [key] resumes its existing schedule rather than restarting the timer, provided the task was left unfinished.
         * - If a task is overdue, it runs once immediately, then resumes its regular schedule.
         *
         * @param key Unique ID for persisting the timer. Leave empty for a non-persistent task.
         * @return A [TaskHandle] that can be used to cancel the task.
         */
        @JvmStatic
        fun performEvery(interval: Long = 0, key: String = "", action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = getActiveOrNull() ?: return handle

            val convertedInterval = realMsToClockMs(interval)

            val state = resolveState(key, Global.getSector().clock.timestamp + convertedInterval, convertedInterval)

            if(key.isNotEmpty()) inst.sectorTimeQueue.removeAll { it.key == key }
            inst.sectorTimeQueue.add(
                TimedTask({ action.run(handle) }, // inject handle into lambda
                    state,
                    handle)
            )

            return handle
        }

        /**
         * Runs [action] when the game is unpaused.
         *
         * If [repeat] is true, the task will be repeated every time the game is unpaused.
         *
         * If [repeat] is false, the task will only run once.
         *
         * Does not persist in save file, re-register every new sector session if needed.
         */
        @JvmStatic
        @JvmOverloads
        fun performOnUnpause(repeat: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val active = getActiveOrNull() ?: return handle

            val task = Task(
                action = { action.run(handle) }, // inject handle into lambda
                repeat = repeat,
                handle = handle
            )

            active.onUnpause.add(task)

            return handle
        }

        /**
         * Runs [action] after the player has left a battle and entered the campaign.
         *
         * If [repeat] is true, the task will be repeated every time the player leaves a battle.
         *
         * If [repeat] is false, the task will only run once.
         *
         * Does not persist in save file, re-register every new sector session if needed.
         */
        @JvmStatic
        @JvmOverloads
        fun performAfterPlayerBattle(repeat: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val active = getActiveOrNull() ?: return handle

            val task = Task(
                action = { action.run(handle) }, // inject handle into lambda
                repeat = repeat,
                handle = handle
            )

            active.afterPlayerBattle.add(task)

            return handle
        }

        internal val onSectorExit = mutableListOf<Task>()
        internal var initAfterFirstAdvance = false

        /**
         * Runs [action] after the player has quit a currently running game and has entered the title screen.
         *
         * If [repeat] is true, the task will be repeated every time the player quits a currently running game.
         *
         * If [repeat] is false, the task will only run once.
         *
         * Does not persist in save file, however this will persist as long as the game is not closed. re-register as needed.
         */
        @JvmStatic
        @JvmOverloads
        fun performAfterSectorExit(repeat: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()

            val task = Task(
                action = { action.run(handle) }, // inject handle into lambda
                repeat = repeat,
                handle = handle
            )

            onSectorExit.add(task)

            return handle
        }

        internal fun battleStarted() {
            active?.battleStarted = true
        }
    }

    private var battleStarted = false
    private val sectorTimeQueue = PriorityQueue<TimedTask>()
    private val onUnpause = mutableListOf<Task>()
    private var wasPaused = false

    private val afterPlayerBattle = mutableListOf<Task>()

    override fun isDone() = false
    override fun runWhilePaused() = true

    private var init = false
    override fun advance(amount: Float) {
        val sector = Global.getSector() ?: return
        val paused = sector.isPaused

        if (!init) {
            initAfterFirstAdvance = false
            init = true
        } else if (!initAfterFirstAdvance) {
            initAfterFirstAdvance = true
        }

        if (battleStarted) {
            runCallbacks(afterPlayerBattle, "onPlayerBattleFinish callback")
            CombatTaskScheduler.setActive(null)
            battleStarted = false
        }

        if (wasPaused && !paused) runCallbacks(onUnpause, "onUnpause callback")
        wasPaused = paused

        drainDueTasks(sectorTimeQueue, sector.clock.timestamp, "deferred sectorTime task")

        SystemTaskScheduler.internalAdvance()
    }
}