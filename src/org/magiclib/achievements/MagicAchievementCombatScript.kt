package org.magiclib.achievements

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI

internal class MagicAchievementCombatScript : BaseEveryFrameCombatPlugin() {
    override fun advance(amount: Float, events: List<InputEventAPI>) {
        MagicAchievementManager.getInstance().advanceInCombat(amount, events)
    }
}
