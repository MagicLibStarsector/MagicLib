package org.magiclib.util.taskScheduler

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.magiclib.util.taskScheduler.TaskSchedulerUtils.safeRun
import java.util.*

/**
 * A task scheduler for timed, recurring, and event-driven actions in combat.
 *
 * Example Java Usage:
 * ```java
 * CombatTaskScheduler.performLater(0L, false, handle -> {
 *     Global.getLogger(this.getClass()).warn("TEST");
 * });
 * ```
 * Example Kotlin Usage:
 * ```kotlin
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

        /**
         * Runs [action] once, [delay] milliseconds from now.
         *
         * If [systemTime] is false, timing follows simulation time (seconds of combat elapsed), and stops advancing while the game is paused.
         *
         * If [systemTime] is true, timing follows [System.nanoTime] in milliseconds, not combat-time, and keeps advancing while the game is paused.
         *
         * Does not persist between battles
         */
        // TODO: try out @IntroducedAt on systemTime for performLater and performEvery (both sector and combat)
        //  to avoid needing to use @JvmOverloads as that would create more functions than I like.
        //  I could manually make a separate function, but not only would that be more needless code, it would also require duplicating the documentation as well.
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

        if (!init) {
            // No Title-Screen
            if (Global.getCurrentState() == GameState.TITLE) {
                if (!SectorTaskScheduler.initAfterFirstAdvance) return // Prevent this from running on load game, as the combat engine starts there for some reason.

                if(SectorTaskScheduler.onSectorExit.isNotEmpty()) {
                    val callbacks = SectorTaskScheduler.onSectorExit.toList()
                    SectorTaskScheduler.onSectorExit.clear()
                    callbacks.forEach {
                        safeRun("onSectorExit callback") { it.action.invoke() }

                        if (!it.repeat || it.handle?.cancelled == true) return@forEach
                        SectorTaskScheduler.onSectorExit.add(it)
                    }
                }

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

            if(onBattleStart.isNotEmpty()) {
                val starters = onBattleStart.toList()
                onBattleStart.clear()
                starters.forEach {
                    safeRun("onPlayerBattleStart callback") { it.action.invoke() }

                    if (!it.repeat || it.handle?.cancelled == true) return@forEach
                    onBattleStart.add(it)
                }
            }

            init = true
        }
        if (Global.getCurrentState() == GameState.TITLE) return

        val engine = this.engine ?: return
        val paused = engine.isPaused

        // detect unpause
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

        if(combatTimeQueue.isNotEmpty()) {
            val combatNow = (engine.getTotalElapsedTime(false) * 1000).toLong()
            while (true) {
                val task = combatTimeQueue.peek() ?: break
                if (task.time >= combatNow) break

                combatTimeQueue.poll()

                if (task.handle?.cancelled == true) continue

                safeRun("deferred combatTime task") { task.action.invoke() }

                task.interval?.let {
                    task.time = combatNow + it
                    combatTimeQueue.add(task)
                }
            }
        }
    }
}