package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.achievements.MagicAchievement
import kotlin.math.roundToInt

abstract class SlayerMagicAchievement : MagicAchievement() {
    var shipsKilled: Int
        get() = memory["shipsKilled"] as? Int ?: 0
        set(value) {
            memory["shipsKilled"] = value
        }

    var listener: Listener? = null

    inner class Listener : BaseCampaignEventListener(false) {
        override fun reportBattleFinished(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
            if (!battle!!.isPlayerInvolved) return

            val recentShipsKilled = battle.nonPlayerSide.sumOf { fleet -> Misc.getSnapshotMembersLost(fleet).count() }
            val involvedFraction = battle.playerInvolvementFraction

            shipsKilled += (recentShipsKilled * involvedFraction).roundToInt()
            this@SlayerMagicAchievement.saveChanges()

            if (shipsKilled > maxProgress) {
                this@SlayerMagicAchievement.completeAchievement()
                this@SlayerMagicAchievement.saveChanges()
                this@SlayerMagicAchievement.onDestroyed()
            }
        }
    }

    override fun onSaveGameLoaded() {
        listener = Listener()
        Global.getSector().addTransientListener(listener)
    }

    override fun onDestroyed() {
        Global.getSector().removeListener(listener)
    }

    override fun getProgress(): Float = shipsKilled.toFloat()
    abstract override fun getMaxProgress(): Float
}

class Slayer100MagicAchievement : SlayerMagicAchievement() {
    override fun getMaxProgress(): Float = 100f
}

class Slayer1000MagicAchievement : SlayerMagicAchievement() {
    override fun getMaxProgress(): Float = 1000f
}

class Slayer2000MagicAchievement : SlayerMagicAchievement() {
    override fun getMaxProgress(): Float = 2000f
}

class Slayer5000MagicAchievement : SlayerMagicAchievement() {
    override fun getMaxProgress(): Float = 5000f
}

class Slayer10000MagicAchievement : SlayerMagicAchievement() {
    override fun getMaxProgress(): Float = 10000f
}