//by Tartiflette

/*
Basic weapon script to apply an interference mechanic. Can be used as a reference or directly pointed to from a weapon file.
The weapon needs an entry in data/config/magicLib/interference.csv
*/

package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.util.MagicInterference;

@Deprecated
public class MagicBasicInterferenceEffect implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (!runOnce) {
            runOnce = true;
            //only affect non built-in
            if (weapon.getShip().getOriginalOwner() < 0 && !weapon.getSlot().isBuiltIn()) {
                MagicInterference.ApplyInterference(weapon.getShip().getVariant());
            }
        }
    }
}