package org.magiclib.bounty.intel

import org.magiclib.bounty.MagicBountyLoader

class MagicBountyBoardProvider: BountyBoardProvider {
    override fun getBounties(): List<BountyInfo> {
        return MagicBountyLoader.BOUNTIES
            .map { (key, spec) ->
                if (spec.job_type == MagicBountyLoader.JobType.Assassination)
                    AssassinationMagicBountyInfo(key, spec)
                else
                    MagicBountyInfo(key, spec)
            }
    }
}