package data.scripts.bounty;

import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

class MagicBountyBattleCreationPlugin extends BattleCreationPluginImpl {
    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
        super.initBattle(context, api);
        if (context.getOtherFleet().getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_FIGHT_TO_THE_LAST)) {
            context.aiRetreatAllowed = false;
            context.fightToTheLast = true; // Should already be set but do it again.
        }
    }
}
