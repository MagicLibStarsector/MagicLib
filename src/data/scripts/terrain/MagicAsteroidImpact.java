package data.scripts.terrain;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicTxt;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

/**
 * @author SafariJohn, Tartiflette
 */
@Deprecated
public class MagicAsteroidImpact implements EveryFrameScript {

    public static float DURATION_SECONDS = 0.2f;

    protected CampaignFleetAPI fleet;
    protected float elapsed;
    protected Vector2f dV;

    public MagicAsteroidImpact(CampaignFleetAPI fleet, FleetMemberAPI target, float damageMult) {
        boolean dealDamage = target != null;

        this.fleet = fleet;
        MutableStat forceMod = fleet.getCommanderStats().getDynamic().getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_FORCE);
        if (forceMod == null) {
            forceMod = new MutableStat(1f);
        }
        MutableStat durationMod = fleet.getCommanderStats().getDynamic().getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DURATION);
        if (durationMod == null) {
            durationMod = new MutableStat(1f);
        }

        Vector2f v = fleet.getVelocity();
        float angle = Misc.getAngleInDegrees(v);
        float speed = v.length();
        if (speed < 10) {
            angle = fleet.getFacing();
        }

        float mult = Misc.getFleetRadiusTerrainEffectMult(fleet);

        float arc = 120f - 60f * mult; // larger fleets suffer more direct collisions that slow them down more

        angle += (float) Math.random() * arc - arc / 2f;
        angle += 180f;

        if (Misc.isSlowMoving(fleet) || mult <= 0) {
            elapsed = DURATION_SECONDS * durationMod.getModifiedValue();
            dV = new Vector2f();
        } else if (fleet.isInCurrentLocation()) {
            if (dealDamage) {
                assert target != null;
                if (damageMult < 1) {
                    damageMult = 1;
                }

                //"af_damage1" : "Asteroid impact",
                //"af_damage2" : " suffers damage from an asteroid impact",
                Misc.applyDamage(target, null, damageMult, true, "asteroid_impact", MagicTxt.getString("af_damage1"),
                        true, null, target.getShipName() + MagicTxt.getString("af_damage2"));
            }

            if (!dealDamage && fleet.isPlayerFleet()) {
                //"af_damage3" : "Asteroid impact on drive bubble",
                Global.getSector().getCampaignUI().addMessage(MagicTxt.getString("af_damage3"), Misc.getNegativeHighlightColor());
            }

            Vector2f test = Global.getSector().getPlayerFleet().getLocation();
            float dist = Misc.getDistance(test, fleet.getLocation());
            if (dist < HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE) {
                float volumeMult = 0.75f;
                volumeMult *= 0.5f + 0.5f * mult;
                if (volumeMult > 0) {
                    if (dealDamage) {
                        Global.getSoundPlayer().playSound("hit_heavy", 1f, volumeMult, fleet.getLocation(), Misc.ZERO);
                    } else {
                        Global.getSoundPlayer().playSound("hit_shield_heavy_gun", 1f, volumeMult, fleet.getLocation(), Misc.ZERO);
                    }
                }
            }

            float size = 10f + (float) Math.random() * 6f;
            size *= 0.67f;
            AsteroidAPI asteroid = fleet.getContainingLocation().addAsteroid(size);
            asteroid.setFacing((float) Math.random() * 360f);
            Vector2f av = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
            av.scale(fleet.getVelocity().length() + (20f + 20f * (float) Math.random()) * mult);
            asteroid.getVelocity().set(av);
            Vector2f al = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
            al.scale(fleet.getRadius());
            Vector2f.add(al, fleet.getLocation(), al);
            asteroid.setLocation(al.x, al.y);

            float sign = Math.signum(asteroid.getRotation());
            asteroid.setRotation(sign * (50f + 50f * (float) Math.random()));

            Misc.fadeInOutAndExpire(asteroid, 0.2f, 1f + (float) Math.random(), 1f);

            Vector2f iv = fleet.getVelocity();
            iv = new Vector2f(iv);
            iv.scale(0.7f);
            float glowSize = 100f + 100f * mult + 50f * (float) Math.random();
            Color color = new Color(255, 165, 100, 255);
            Misc.addHitGlow(fleet.getContainingLocation(), al, iv, glowSize, color);
        }

        dV = Misc.getUnitVectorAtDegreeAngle(angle);

        float impact = speed * 1f * (0.5f + mult * 0.5f) * forceMod.getModifiedValue();
        dV.scale(impact);
        dV.scale(1f / DURATION_SECONDS);
    }

    @Override
    public void advance(float amount) {

        fleet.setOrbit(null);

        Vector2f v = fleet.getVelocity();
        fleet.setVelocity(v.x + dV.x * amount, v.y + dV.y * amount);

        elapsed += amount;
    }

    @Override
    public boolean isDone() {
        MutableStat durationMod = fleet.getCommanderStats().getDynamic().getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DURATION);
        if (durationMod == null) {
            durationMod = new MutableStat(1f);
        }
        return elapsed >= DURATION_SECONDS * durationMod.getModifiedValue();
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

}
