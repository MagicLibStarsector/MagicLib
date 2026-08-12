package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignClockAPI
import org.magiclib.util.taskScheduler.TaskSchedulerUtils.safeRun
import java.util.*

/**
 * A task scheduler for timed, recurring, and event-driven actions in the sector.
 *
 * Example Java Usage:
 * ```java
 * SectorTaskScheduler.performLater(0L, false, handle -> {
 *     Global.getLogger(this.getClass()).warn("TEST");
 * });
 * ```
 * Example Kotlin Usage:
 * ```kotlin
 * SectorTaskScheduler.performLater(0L, false) { handle ->
 *     Global.getLogger(this::class.java).warn("TEST")
 * }
 * ```
 *
 * @author S-Numan
 */
class SectorTaskScheduler : EveryFrameScript {

    companion object {
        private var active: SectorTaskScheduler? = null
        internal fun setActive(value: SectorTaskScheduler?) {
            active = value
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
         * If [systemTime] is false, timing follows the sector clock and stops advancing while the game is paused. Elapsed real time will differ from [delay] if the game's time acceleration
         * (e.g. fast-forward) is active, or if a mod changes [CampaignClockAPI.getSecondsPerDay].
         *
         * If [systemTime] is true, timing follows [System.nanoTime] in milliseconds, not sector-days, and keeps advancing while the game is paused.
         *
         * Does not persist in save file, re-register every session if needed.
         */
        @JvmStatic
        fun performLater(delay: Long = 0, systemTime: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = active ?: return handle

            val task = TimedTask(
                action = { action.run(handle) },
                time =
                    if (systemTime) System.nanoTime() + delay * 1_000_000L
                    else Global.getSector().clock.timestamp + realMsToClockMs(delay),
                interval = null,
                handle = handle
            )

            if (systemTime)
                inst.systemTimeQueue.add(task)
            else
                inst.sectorTimeQueue.add(task)

            return handle
        }

        /**
         * Runs [action] every [interval] milliseconds, starting after one interval.
         *
         * If [systemTime] is false, timing follows the sector clock and stops advancing while the game is paused. Elapsed real time will differ from [interval] if the game's time acceleration
         * (e.g. fast-forward) is active, or if a mod changes [CampaignClockAPI.getSecondsPerDay].
         *
         * If [systemTime] is true, timing follows [System.nanoTime] in milliseconds, not sector-days, and keeps advancing while the game is paused.
         *
         * Does not persist in save file, re-register every session if needed.
         */
        @JvmStatic
        fun performEvery(interval: Long = 0, systemTime: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = active ?: return handle

            val convertedInterval = if (systemTime) interval * 1_000_000L else realMsToClockMs(interval)

            val task = TimedTask(
                action = { action.run(handle) }, // inject handle into lambda
                time =
                    if (systemTime) System.nanoTime() + convertedInterval
                    else Global.getSector().clock.timestamp + convertedInterval,
                interval = convertedInterval,
                handle = handle
            )

            if (systemTime)
                inst.systemTimeQueue.add(task)
            else
                inst.sectorTimeQueue.add(task)

            return handle
        }

        /**
         * Runs [action] when the game is unpaused.
         *
         * If [repeat] is true, the task will be repeated every time the game is unpaused.
         *
         * If [repeat] is false, the task will only run once.
         *
         * Does not persist in save file, re-register every session if needed.
         */
        @JvmStatic
        @JvmOverloads
        fun performOnUnpause(repeat: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()

            val task = Task(
                action = { action.run(handle) }, // inject handle into lambda
                repeat = repeat,
                handle = handle
            )

            active?.onUnpause?.add(task)

            return handle
        }

        /**
         * Runs [action] after the player has left a battle and entered the campaign.
         *
         * If [repeat] is true, the task will be repeated every time the player leaves a battle.
         *
         * If [repeat] is false, the task will only run once.
         *
         * Does not persist in save file, re-register every session if needed.
         */
        @JvmStatic
        @JvmOverloads
        fun performAfterPlayerBattle(repeat: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()

            val task = Task(
                action = { action.run(handle) }, // inject handle into lambda
                repeat = repeat,
                handle = handle
            )

            active?.afterPlayerBattle?.add(task)

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
    private val systemTimeQueue = PriorityQueue<TimedTask>()
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
            CombatTaskScheduler.onBattleStart.clear()

            initAfterFirstAdvance = false
            init = true
        } else if (!initAfterFirstAdvance) {
            initAfterFirstAdvance = true
        }

        if (battleStarted && afterPlayerBattle.isNotEmpty()) {
            val callbacks = afterPlayerBattle.toList()
            afterPlayerBattle.clear()
            callbacks.forEach {
                safeRun("onPlayerBattleFinish callback") { it.action.invoke() }

                if (!it.repeat || it.handle?.cancelled == true) return@forEach
                afterPlayerBattle.add(it)
            }
        }
        battleStarted = false

        if (wasPaused && !paused && onUnpause.isNotEmpty()) {
            val callbacks = onUnpause.toList()
            onUnpause.clear()
            callbacks.forEach {
                safeRun("onUnpause callback") { it.action.invoke() }

                if (!it.repeat || it.handle?.cancelled == true) return@forEach
                onUnpause.add(it)
            }
        }
        wasPaused = paused

        if(systemTimeQueue.isNotEmpty()) {
            val systemNow = System.nanoTime()
            while (true) {
                val task = systemTimeQueue.peek() ?: break
                if (task.time >= systemNow) break

                systemTimeQueue.poll()

                if (task.handle?.cancelled == true) continue

                safeRun("deferred systemTime task") { task.action.invoke() }

                task.interval?.let {
                    task.time = systemNow + it
                    systemTimeQueue.add(task)
                }
            }
        }

        if(sectorTimeQueue.isNotEmpty()) {
            val sectorNow = sector.clock.timestamp
            while (true) {
                val task = sectorTimeQueue.peek() ?: break
                if (task.time >= sectorNow) break

                sectorTimeQueue.poll()

                if (task.handle?.cancelled == true) continue

                safeRun("deferred sectorTime task") { task.action.invoke() }

                task.interval?.let {
                    task.time = sectorNow + it
                    sectorTimeQueue.add(task)
                }
            }
        }
    }
}