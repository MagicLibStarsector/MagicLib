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
 * CombatTaskScheduler.performLater(0L, handle -> {
 *     Global.getLogger(this.getClass()).warn("TEST");
 * });
 * ```
 * Example Kotlin Usage:
 * ```
 * CombatTaskScheduler.performLater(0L) { handle ->
 *     Global.getLogger(this::class.java).warn("TEST")
 * }
 * ```
 *
 * @author S-Numan
 */
class CombatTaskScheduler : BaseEveryFrameCombatPlugin() {

    companion object {
        private var active: CombatTaskScheduler? = null
        internal fun setActive(value: CombatTaskScheduler?) {
            active = value
        }
        private fun getActiveOrNull(): CombatTaskScheduler? {
            if(active == null) {
                val caller = Throwable().stackTrace.getOrNull(1)
                val callerName = caller?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" } ?: "unknown"
                Global.getLogger(this::class.java).warn("Function [$callerName] called when not in combat. Cannot add task.")
            }
            return active
        }

        @JvmStatic
        fun getCombatTasks(): Collection<TimedTask> {
            return active?.combatTimeQueue?.sorted() ?: emptyList()
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
         * Timing follows simulation time (seconds of combat elapsed), and stops advancing while the game is paused.
         *
         * Does not persist between battles
         */
        @JvmStatic
        fun performLater(delay: Long = 0, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = getActiveOrNull() ?: return handle
            val engine = inst.engine ?: return handle

            val task = TimedTask(
                action = { action.run(handle) },
                TaskState(
                    time = (engine.getTotalElapsedTime(false) * 1000).toLong() + delay,
                    interval = null
                ),
                handle = handle,
            )

            inst.combatTimeQueue.add(task)

            return handle
        }

        /**
         * Runs [action] every [interval] milliseconds, starting after one interval.
         *
         * Timing follows simulation time (seconds of combat elapsed), and stops advancing while the game is paused.
         *
         * Does not persist between battles
         */
        @JvmStatic
        fun performEvery(interval: Long = 0, action: TaskSchedulerUtils.TaskAction): TaskHandle {
            val handle = TaskHandle()
            val inst = getActiveOrNull() ?: return handle
            val engine = inst.engine ?: return handle

            val task = TimedTask(
                action = { action.run(handle) }, // inject handle into lambda
                TaskState(
                    time = (engine.getTotalElapsedTime(false) * 1000).toLong() + interval,
                    interval = interval
                ),
                handle = handle
            )

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
            val active = getActiveOrNull() ?: return handle

            val task = Task(
                action = { action.run(handle) }, // inject handle into lambda
                repeat = repeat,
                handle = handle
            )

            active.onUnpause.add(task)

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
    private val onUnpause = mutableListOf<Task>()
    private var wasPaused = false

    private var init = false
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (!init) {
            if (Global.getCurrentState() == GameState.TITLE) {
                // GameState.TITLE
                if (!SectorTaskScheduler.initAfterFirstAdvance) return // Prevent this from running on load game, as the combat engine starts there for some reason.

                runCallbacks(SectorTaskScheduler.onSectorExit, "onSectorExit callback")

                onBattleStart.clear()

                SectorTaskScheduler.initAfterFirstAdvance = false
                SectorTaskScheduler.setActive(null)
                init = true
            } else {
                // GameState.COMBAT
                this.engine = Global.getCombatEngine()
                if (engine != null) {
                    combatTimeQueue.clear()

                    onUnpause.clear()
                    wasPaused = false

                    active = this

                    SectorTaskScheduler.battleStarted()

                    runCallbacks(onBattleStart, "onPlayerBattleStart callback")

                    init = true
                }
            }
        }
        if (Global.getCurrentState() != GameState.TITLE) {
            val engine = this.engine
            if (engine != null) {
                val paused = engine.isPaused

                // detect unpause
                if (wasPaused && !paused) runCallbacks(onUnpause, "onUnpause callback")
                wasPaused = paused

                drainDueTasks(combatTimeQueue, (engine.getTotalElapsedTime(false) * 1000).toLong(), "deferred combatTime task")
            }
        }

        SystemTaskScheduler.internalAdvance()
    }
}