package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.CoreScript
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec

/**
 * From vanilla: [CoreScript.getCargoCommodities]
 * @since 1.3.0
 */
fun CargoAPI.getCargoCommodities(): Set<String> = CoreScript.getCargoCommodities(this)

/**
 * From vanilla: [CoreScript.addMiscToDropData]
 * @since 1.3.0
 */
fun SalvageEntityGenDataSpec.DropData.addMiscToDropData(
    member: FleetMemberAPI,
    weapons: Boolean,
    mods: Boolean,
    fighters: Boolean
) = CoreScript.addMiscToDropData(this, member, weapons, mods, fighters)

/**
 * From vanilla: [CoreScript.markSystemAsEntered]
 * @since 1.3.0
 */
fun StarSystemAPI.markSystemAsEntered(withMessages: Boolean) = CoreScript.markSystemAsEntered(this, withMessages)

/**
 * From vanilla: [CoreScript.generateOrAddToDebrisFieldFromBattle]
 * @since 1.3.0
 */
fun BattleAPI.generateOrAddToDebrisFieldFromBattle(primaryWinner: CampaignFleetAPI) =
    CoreScript.generateOrAddToDebrisFieldFromBattle(primaryWinner, this)