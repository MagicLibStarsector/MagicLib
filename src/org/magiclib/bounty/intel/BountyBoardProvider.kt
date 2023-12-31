package org.magiclib.bounty.intel

interface BountyBoardProvider {
    fun getBounties(): List<BountyInfo>
}