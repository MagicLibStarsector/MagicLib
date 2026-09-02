package org.magiclib.bounty

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import org.magiclib.bounty.ActiveBounty.BountyResult.ExpiredWithoutAccepting

/**
 * Accepts bounty upon becoming visible to the player fleet
 */
class MagicBountyFleetListener(val bounty: ActiveBounty): EveryFrameScript {
    var done = false
    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false
    override fun advance(amount: Float) {
        if(!bounty.isDespawning && bounty.getDaysRemainingToComplete() <= 0)  {
            Global.getLogger(this.javaClass).info(String.format("Ending expired bounty without accepting %s", bounty.key))
            if (bounty.stage == ActiveBounty.Stage.NotAccepted) bounty.endBounty(ExpiredWithoutAccepting())
            //else bounty.endBounty(ExpiredAfterAccepting())
        }
        if(!bounty.fleet.isInCurrentLocation) return
        if(!bounty.fleet.isVisibleToPlayerFleet) return
        if(bounty.fleet.visibilityLevelToPlayerFleet != SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS
            && bounty.fleet.visibilityLevelToPlayerFleet != SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS)
            return

        if(bounty.stage == ActiveBounty.Stage.NotAccepted) {
            bounty.acceptBounty(
                Global.getSector().playerFleet,
                bounty.spec.job_reputation_reward,
                bounty.spec.job_forFaction
            )
            done = true
        }
    }
}