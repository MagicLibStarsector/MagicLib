package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.magiclib.util.taskScheduler.TaskSchedulerUtils.drainDueTasks
import org.magiclib.util.taskScheduler.TaskSchedulerUtils.runCallbacks
import java.util.*

/**
 * A task scheduler for timed, recurring, and event-driven actions in combat.
 *
 * Example Java Usage:
 * ```
 * CombatTaskScheduler.performLater(0L, false, handle -> {
 *     Global.getLogger(this.getClass()).warn("TEST");
 * });
 * ```
 * Example Kotlin Usage:
 * ```
 * CombatTaskScheduler.performLater(0L, false) { handle ->
 *     Global.getLogger(this::class.java).warn("TEST")
 * }
 * ```
 *
 * @author S-Numan
 */
class CombatTaskScheduler : BaseEveryFrameCombatPlugin() {

    companion object {
        private var active: CombatTaskScheduler? = null

        @JvmStatic
        fun getSystemTasks(): Collection<TimedTask> {
            return active?.systemTimeQueue?.toList() ?: emptyList()
        }

        @JvmStatic
        fun getCombatTasks(): Collection<TimedTask> {
            return active?.combatTimeQueue?.toList() ?: emptyList()
        }

        @JvmStatic
        fun getOnUnpauseTasks(): Collection<Task> {
            return active?.onUnpause?.toList() ?: emptyList()
        }

        @JvmStatic
        fun getOnBattleStartTasks(): Collection<Task> {
            return onBattleStart.toList()
        }

        /**
         * Runs [action] once, [delay] milliseconds from now.
         *
         * If [systemTime] is false, timing follows simulation time (seconds of combat elapsed), and stops advancing while the game is paused.
         *
         * If [systemTime] is true, timing follows [System.nanoTime] in milliseconds, not combat-time, and keeps advancing while the game is paused.
         *
         * Does not persist between battles
         */
        @JvmStatic
        fun performLater(delay: Long = 0, systemTime: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = active ?: return handle
            val engine = inst.engine ?: return handle

            val task = TimedTask(
                action = { action.run(handle) },
                time =
                    if (systemTime) System.nanoTime() + delay * 1_000_000L
                    else (engine.getTotalElapsedTime(false) * 1000).toLong() + delay,
                interval = null,
                handle = handle
            )

            if (systemTime)
                inst.systemTimeQueue.add(task)
            else
                inst.combatTimeQueue.add(task)

            return handle
        }

        /**
         * Runs [action] every [interval] milliseconds, starting after one interval.
         *
         * If [systemTime] is false, timing follows simulation time (seconds of combat elapsed), and stops advancing while the game is paused.
         *
         * If [systemTime] is true, timing follows [System.nanoTime] in milliseconds, not combat-time, and keeps advancing while the game is paused.
         *
         * Does not persist between battles
         */
        @JvmStatic
        fun performEvery(interval: Long = 0, systemTime: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = active ?: return handle
            val engine = inst.engine ?: return handle

            val task = TimedTask(
                action = { action.run(handle) }, // inject handle into lambda
                time =
                    if (systemTime) System.nanoTime() + interval * 1_000_000L
                    else (engine.getTotalElapsedTime(false) * 1000).toLong() + interval,
                interval = if (systemTime) interval * 1_000_000L else interval,
                handle = handle
            )

            if (systemTime)
                inst.systemTimeQueue.add(task)
            else
                inst.combatTimeQueue.add(task)

            return handle
        }

        /**
         * Runs [action] when the game is unpaused.
         *
         * If [repeat] is true, the task will be repeated every time the game is unpaused.
         *
         * If [repeat] is false, the task will only run once.
         *
         * Does not persist between battles
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

        internal val onBattleStart = mutableListOf<Task>()

        /**
         * Runs [action] on player entering a battle and CombatEngine existing.
         *
         * If [repeat] is true, the task will be repeated every time the player enters a battle.
         *
         * If [repeat] is false, the task will only run once.
         *
         * Will persist between battles, even if not currently in a battle. Does not persist in save file, re-register every session if needed.
         */
        @JvmStatic
        @JvmOverloads
        fun performOnPlayerBattleStart(repeat: Boolean = false, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()

            val task = Task(
                action = { action.run(handle) }, // inject handle into lambda
                repeat = repeat,
                handle = handle
            )

            onBattleStart.add(task)

            return handle
        }
    }

    private var engine: CombatEngineAPI? = null

    private val combatTimeQueue = PriorityQueue<TimedTask>()
    private val systemTimeQueue = PriorityQueue<TimedTask>()
    private val onUnpause = mutableListOf<Task>()
    private var wasPaused = false

    private var init = false
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        SystemTaskScheduler.internalAdvance()

        if (!init) {
            // No Title-Screen
            if (Global.getCurrentState() == GameState.TITLE) {
                if (!SectorTaskScheduler.initAfterFirstAdvance) return // Prevent this from running on load game, as the combat engine starts there for some reason.

                runCallbacks(SectorTaskScheduler.onSectorExit, "onSectorExit callback")

                onBattleStart.clear()

                SectorTaskScheduler.initAfterFirstAdvance = false
                init = true
                return
            }
            // No Title-Screen

            this.engine = Global.getCombatEngine() ?: return
            systemTimeQueue.clear()
            combatTimeQueue.clear()

            onUnpause.clear()
            wasPaused = false

            active = this

            SectorTaskScheduler.battleStarted()

            runCallbacks(onBattleStart, "onPlayerBattleStart callback")

            init = true
        }
        if (Global.getCurrentState() == GameState.TITLE) return

        val engine = this.engine ?: return
        val paused = engine.isPaused

        // detect unpause
        if (wasPaused && !paused) runCallbacks(onUnpause, "onUnpause callback")
        wasPaused = paused

        drainDueTasks(systemTimeQueue, System.nanoTime(), "deferred systemTime task")
        drainDueTasks(combatTimeQueue, (engine.getTotalElapsedTime(false) * 1000).toLong(), "deferred combatTime task")
    }
}