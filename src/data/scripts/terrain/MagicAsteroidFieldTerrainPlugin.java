package data.scripts.terrain;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.RingSystemTerrainPlugin;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.util.MagicTxt;

import java.awt.*;
import java.util.Random;

import static data.scripts.terrain.MagicAsteroidBeltTerrainPlugin.*;

/**
 * This is a drop-in replacement for the vanilla AsteroidFieldTerrainPlugin.
 * <p>
 * For example usage, see Roider Union by SafariJohn.
 *
 * @author SafariJohn, Tartiflette
 */
@Deprecated
public class MagicAsteroidFieldTerrainPlugin extends AsteroidFieldTerrainPlugin {

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);

        params = (AsteroidFieldParams) param;
        name = params.name;
        if (name == null) {
            //"af_fieldName" : "Asteroid Field",
            name = MagicTxt.getString("af_fieldName");
        }
        params.numAsteroids = params.minAsteroids;
        if (params.maxAsteroids > params.minAsteroids) {
            params.numAsteroids += new Random().nextInt(params.maxAsteroids - params.minAsteroids);
        }
    }

    @Override
    public void renderOnMap(float factor, float alphaMult) {
    }

    @Override
    public void regenerateAsteroids() {
        createAsteroidField();
    }

    protected void createAsteroidField() {
        if (!(params instanceof AsteroidFieldParams)) {
            return;
        }

        Random rand = new Random(Global.getSector().getClock().getTimestamp() + entity.getId().hashCode());

        float fieldRadius = params.minRadius + (params.maxRadius - params.minRadius) * rand.nextFloat();
        params.bandWidthInEngine = fieldRadius;
        params.middleRadius = fieldRadius / 2f;

        LocationAPI location = entity.getContainingLocation();
        if (location == null) {
            return;
        }
        for (int i = 0; i < params.numAsteroids; i++) {
            float size = params.minSize + (params.maxSize - params.minSize) * rand.nextFloat();
            AsteroidAPI asteroid = location.addAsteroid(size);
            asteroid.setFacing(rand.nextFloat() * 360f);

            float r = rand.nextFloat();
            r = 1f - r * r;

            float currRadius = fieldRadius * r;

            float minOrbitDays = Math.max(1f, currRadius * 0.05f);
            float maxOrbitDays = Math.max(2f, currRadius * 2f * 0.05f);
            float orbitDays = minOrbitDays + rand.nextFloat() * (maxOrbitDays - minOrbitDays);

            float angle = rand.nextFloat() * 360f;
            asteroid.setCircularOrbit(this.entity, angle, currRadius, orbitDays);
            Misc.setAsteroidSource(asteroid, this);
        }
        needToCreateAsteroids = false;
    }

    @Override
    public void advance(float amount) {
        if (needToCreateAsteroids) {
            createAsteroidField();
        }
        super.advance(amount);
    }

    @Override
    public String getNameForTooltip() {
        //"af_fieldName" : "Asteroid Field",
        return MagicTxt.getString("af_fieldName");
    }

    @Override
    public void reportAsteroidPersisted(SectorEntityToken asteroid) {
        if (Misc.getAsteroidSource(asteroid) == this) {
            params.numAsteroids--;
        }
    }

    @Override
    protected Object readResolve() {
        super.readResolve();
        return this;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        super.render(layer, viewport);
    }

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        CampaignFleetAPI fleet;
        if (entity instanceof CampaignFleetAPI) {
            fleet = (CampaignFleetAPI) entity;
        } else {
            return;
        }

        if (Misc.isSlowMoving(fleet)) {
            fleet.getStats().addTemporaryModMult(0.1f, getModId() + "_2",
                    //"af_hiding" : "Hiding inside ",
                    MagicTxt.getString("af_hiding") + getNameForTooltip().toLowerCase(),
                    RingSystemTerrainPlugin.getVisibilityMult(fleet),
                    fleet.getStats().getDetectedRangeMod());
        }

        if (fleet.isInHyperspaceTransition()) {
            return;
        }

        MutableStat chanceMod = fleet.getCommanderStats().getDynamic().getStat(IMPACT_CHANCE);
        if (chanceMod == null) {
            chanceMod = new MutableStat(1f);
        }
        MutableStat damageChanceMod = fleet.getCommanderStats().getDynamic().getStat(IMPACT_DAMAGE_CHANCE);
        if (damageChanceMod == null) {
            damageChanceMod = new MutableStat(1f);
        }
        MutableStat damageMod = fleet.getCommanderStats().getDynamic().getStat(IMPACT_DAMAGE);
        if (damageMod == null) {
            damageMod = new MutableStat(1f);
        }

        MemoryAPI mem = fleet.getMemoryWithoutUpdate();
        if (mem.contains(IMPACT_TIMEOUT)) {
            return;
        }

        float expire = mem.getExpire(IMPACT_SKIPPED);
        if (expire < 0) {
            expire = 0;
        }

        float hitProb = expire / DURATION_PER_SKIP * PROB_PER_SKIP;
        hitProb *= chanceMod.getModifiedValue();

        if (hitProb > MAX_PROBABILITY) {
            hitProb = MAX_PROBABILITY;
        }

        boolean impact = (float) Math.random() < hitProb;

        if (impact) {
            FleetMemberAPI target = null;
            float damageMult = fleet.getCurrBurnLevel() - Misc.getGoSlowBurnLevel(fleet);

            boolean doDamage = mem.is(IMPACT_RECENT, true);
            doDamage &= (float) Math.random() * damageChanceMod.getModifiedValue() > 0.5f;

            if (doDamage) {
                WeightedRandomPicker<FleetMemberAPI> targets = new WeightedRandomPicker<>();
                for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
                    float w = 1f;
                    switch (m.getHullSpec().getHullSize()) {
                        case CAPITAL_SHIP:
                            w = 20f;
                            break;
                        case CRUISER:
                            w = 10f;
                            break;
                        case DESTROYER:
                            w = 5f;
                            break;
                        case FRIGATE:
                            w = 1f;
                            break;
                    }

                    MutableStat memberDamageChanceMod = m.getStats().getDynamic().getStat(IMPACT_DAMAGE_CHANCE);
                    if (memberDamageChanceMod == null) {
                        memberDamageChanceMod = new MutableStat(1f);
                    }

                    w *= memberDamageChanceMod.getModifiedValue();

                    targets.add(m, w);
                }

                target = targets.pick();

                if (target != null) {

                    damageMult *= damageMod.getModifiedValue();

                    MutableStat memberDamageMod = target.getStats().getDynamic().getStat(IMPACT_DAMAGE);
                    if (memberDamageMod == null) {
                        memberDamageMod = new MutableStat(1f);
                    }

                    damageMult *= memberDamageMod.getModifiedValue();
                }
            }

            fleet.addScript(new MagicAsteroidImpact(fleet, target, damageMult));

            float damageWindow = MIN_DAMAGE_WINDOW;
            damageWindow += (float) Math.random();

            mem.set(IMPACT_SKIPPED, true, 0);
            mem.set(IMPACT_RECENT, true, damageWindow);
        } else {
            mem.set(IMPACT_SKIPPED, true, Math.min(expire + DURATION_PER_SKIP,
                    MAX_SKIPS_TO_TRACK * DURATION_PER_SKIP));
        }

        float timeout = MIN_TIMEOUT + (MAX_TIMEOUT - MIN_TIMEOUT) * (float) Math.random();
        mem.set(IMPACT_TIMEOUT, true, timeout);
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public String getNameAOrAn() {
        return MagicTxt.getString("an");
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        float pad = 10f;
        float small = 5f;
        Color highlight = Misc.getHighlightColor();

        tooltip.addTitle(getNameForTooltip());
        tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).getText1(), pad);

        float nextPad = pad;
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad);
            nextPad = small;
        }
        //"af_impact1" : "Chance of asteroid impacts that briefly knock the fleet off course and ",
        //"af_impact2" : "may occasionally impact ships directly, dealing moderate damage.",
        tooltip.addPara(MagicTxt.getString("af_impact1")
                + MagicTxt.getString("af_impact2"), nextPad);
        //"af_impact3" : "Smaller fleets are usually able to avoid the heavier impacts, ",
        //"af_impact4" : "and slow-moving fleets do not risk impacts at all.",
        //"af_impact5" : "slow-moving",
        tooltip.addPara(MagicTxt.getString("af_impact3")
                        + MagicTxt.getString("af_impact4"), pad,
                highlight,
                MagicTxt.getString("af_impact5")
        );

        String stop = Global.getSettings().getControlStringForEnumName("GO_SLOW");

        //"af_sensor1" : "Reduces the range at which stationary or slow-moving* fleets inside it can be detected by %s.",
        tooltip.addPara(MagicTxt.getString("af_sensor1"), nextPad,
                highlight,
                (int) ((1f - RingSystemTerrainPlugin.getVisibilityMult(Global.getSector().getPlayerFleet())) * 100) + MagicTxt.getString("%")
        );
        //"af_sensor2" : "*Press and hold %s to stop; combine with holding the left mouse button down to move slowly. ",
        //"af_sensor3" : "A slow-moving fleet moves at a burn level of half that of its slowest ship.",
        tooltip.addPara(MagicTxt.getString("af_sensor2")
                        + MagicTxt.getString("af_sensor3"), nextPad,
                Misc.getGrayColor(), highlight,
                stop
        );

        if (expanded) {
            //"af_combat1" : "Combat",
            //"af_combat2" : "Numerous asteroids present on the battlefield. Large enough to be an in-combat navigational hazard.",
            tooltip.addSectionHeading(MagicTxt.getString("af_combat1"), Alignment.MID, pad);
            tooltip.addPara(MagicTxt.getString("af_combat2"), small);
        }
    }

    @Override
    public boolean isTooltipExpandable() {
        return true;
    }

    @Override
    public float getTooltipWidth() {
        return 350f;
    }

    @Override
    public String getEffectCategory() {
        return "asteroid_belt";
    }

    @Override
    public boolean hasAIFlag(Object flag) {
        return flag == TerrainAIFlags.REDUCES_SPEED_LARGE || flag == TerrainAIFlags.DANGEROUS_UNLESS_GO_SLOW;
    }
}
