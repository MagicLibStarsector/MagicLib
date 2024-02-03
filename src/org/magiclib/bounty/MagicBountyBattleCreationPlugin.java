package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class MagicBountyBattleCreationPlugin extends BattleCreationPluginImpl {
    @NotNull
    private final String bountyKey;

    public MagicBountyBattleCreationPlugin(@NotNull String bountyKey) {
        this.bountyKey = bountyKey;
    }

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
        super.initBattle(context, api);
        if (context.getOtherFleet().getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_FIGHT_TO_THE_LAST)) {
            context.aiRetreatAllowed = false;
            context.fightToTheLast = true; // Should already be set but do it again.
        }
    }

    @Override
    public void afterDefinitionLoad(final CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);

        final String musicSetId = MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey).getSpec().fleet_musicSetId;

        if (musicSetId != null && !musicSetId.isEmpty()) {
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                boolean played = false;
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (played || engine.isPaused()) return;

                    if (engine.getTotalElapsedTime(false) > 1f) {
                        try {
                            Global.getSoundPlayer().playCustomMusic(1, 1, musicSetId, true);
                        } catch (Exception e) {
                            Global.getLogger(this.getClass()).warn("Failed to play music set " + musicSetId, e);
                        }

                        played = true;
                    }
                }
            });
        }

//        String script = "org.magiclib.bounty.MagicBountyBattleCreationPlugin"; //MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey).getCustomEveryFrameCombatPluginClass();
//
//        if (script == null) return;
//
//        Object scriptInstance;
//
//        try {
//            scriptInstance = Class.forName(script).getDeclaredConstructor().newInstance();
//        } catch (InstantiationException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
//                 IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }
//        engine.addPlugin((EveryFrameCombatPlugin) scriptInstance);
    }
}
