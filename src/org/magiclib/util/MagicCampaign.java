package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.campaign.MagicCaptainBuilder;
import org.magiclib.campaign.MagicFleetBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.util.Misc.MAX_OFFICER_LEVEL;

/**
 * A collection of methods to help do a variety of things on the campaign layer.
 */
public class MagicCampaign {

    protected static Logger log = Global.getLogger(MagicCampaign.class);


    /////////////////////////
    //                     //
    //   FLEET GEN STUFF   //
    //                     //
    /////////////////////////

    /**
     * Creates a fleet with a defined flagship and optional escort.
     * <p>
     * Not all fields are required. Each `set` method has a comment showing the default value for if it is not used.
     * <p>
     * Example usage:
     * <pre>
     * MagicCampaign.createFleetBuilder()
     *         .setFleetName("Hegemony Attack Fleet")
     *         .setFleetFaction(Factions.HEGEMONY)
     *         .setFleetType(FleetTypes.TASK_FORCE)
     *         .setFlagshipName("HSS Onslaught")
     *         .setFlagshipVariant("onslaught_xiv_Elite")
     *         .setFlagshipAlwaysRecoverable(false)
     *         .setFlagshipAutofit(true)
     *         .setCaptain(theCaptain)
     *         .setSupportAutofit(true)
     *         .setReinforcementFaction(Factions.HEGEMONY)
     *         .create()
     * </pre>
     *
     * @author Wisp
     * @since 0.46.0
     */
    public static MagicFleetBuilder createFleetBuilder() {
        return new MagicFleetBuilder();
    }

    /**
     * Creates a captain PersonAPI.
     * <p>
     * Not all fields are required. Each `set` method has a comment showing the default value for if it is not used.
     * <p>
     * Usage:
     * <pre>
     * MagicCampaign.createCaptainBuilder(Factions.LUDDIC_CHURCH)
     *             .setFirstName("David")
     *             .setLastName("Rengel")
     *             .setGender(FullName.Gender.MALE)
     * </pre>
     *
     * @since 0.46.1
     */
    public static MagicCaptainBuilder createCaptainBuilder(@NotNull String factionId) {
        return new MagicCaptainBuilder(factionId);
    }

    /**
     * Creates a ship variant from a regular variant file.
     * Used to create variants that requires different mods to be loaded.
     *
     * @param path variant file full path.
     * @return ship variant object
     */
    //Credit to Rubi
    public static ShipVariantAPI loadVariant(String path) {
        ShipVariantAPI variant = null;
        try {
            JSONObject obj = Global.getSettings().loadJSON(path);
            String displayName = obj.getString("displayName");
            int fluxCapacitors = obj.getInt("fluxCapacitors");
            int fluxVents = obj.getInt("fluxVents");
            boolean goalVariant = false;
            try {
                goalVariant = obj.getBoolean("goalVariant");
            } catch (JSONException ignored) {
            }
            String hullId = obj.getString("hullId");
            JSONArray hullMods = obj.getJSONArray("hullMods");
            JSONArray modules = null;
            try {
                modules = obj.getJSONArray("modules");
            } catch (JSONException ignored) {
            }
            JSONArray permaMods = obj.getJSONArray("permaMods");
            JSONArray sMods = null;
            try {
                sMods = obj.getJSONArray("sMods");
            } catch (JSONException ignored) {
            }
            //float quality = (float) obj.getDouble("quality"); not used/available in API
            String variantId = obj.getString("variantId");
            JSONArray weaponGroups = obj.getJSONArray("weaponGroups");
            JSONArray wings = null;
            try {
                wings = obj.getJSONArray("wings");
            } catch (JSONException ignored) {
            }

            variant = Global.getSettings().createEmptyVariant(variantId, Global.getSettings().getHullSpec(hullId));
            variant.setVariantDisplayName(displayName);
            variant.setNumFluxCapacitors(fluxCapacitors);
            variant.setNumFluxVents(fluxVents);
            variant.setGoalVariant(goalVariant);
            // todo: check if order matters
            if (sMods != null) {
                for (int k = 0; k < sMods.length(); k++) {
                    String sModId = hullMods.getString(k);
                    variant.addPermaMod(sModId, true);
//                    variant.addPermaMod(sModId);
                    variant.addMod(sModId);
                }
            }
            if (permaMods != null) {
                for (int j = 0; j < permaMods.length(); j++) {
                    String permaModId = hullMods.getString(j);
                    variant.addPermaMod(permaModId);
                    if (!variant.getHullMods().contains(permaModId)) {
                        variant.addMod(permaModId);
                    }
                }
            }
            if (hullMods != null) {
                for (int i = 0; i < hullMods.length(); i++) {
                    String hullModId = hullMods.getString(i);
                    if (!variant.getHullMods().contains(hullModId)) {
                        variant.addMod(hullModId);
                    }
                }
            }
            if (modules != null) {
                for (int m = 0; m < modules.length(); m++) {
                    JSONObject module = modules.getJSONObject(m);
                    // todo this is a very inefficient way to do it (obj length always == 1)
                    //  but I don't want to deal with Iterators
                    JSONArray slots = module.names();
                    for (int s = 0; s < slots.length(); s++) {
                        String slotId = slots.getString(s);
                        String moduleVariantId = module.getString(slotId);
                        //todo *** Given moduleVariantId instead of path, create ShipVariantAPI using loadVariant() ***
                        variant.setModuleVariant(slotId, Global.getSettings().getVariant(moduleVariantId));
                    }
                }
            }
            // todo maybe you can do something better with variant.getNonBuiltInWeaponSlots()?
            for (int wg = 0; wg < weaponGroups.length(); wg++) {
                WeaponGroupSpec weaponGroupSpec = new WeaponGroupSpec(WeaponGroupType.LINKED);
                JSONObject weaponGroup = weaponGroups.getJSONObject(wg);
                boolean autofire = weaponGroup.getBoolean("autofire");
                String mode = weaponGroup.getString("mode");
                JSONObject weapons = weaponGroup.getJSONObject("weapons");
                JSONArray slots = weapons.names();
                for (int s = 0; s < slots.length(); s++) {
                    String slotId = slots.getString(s);
                    String weaponId = weapons.getString(slotId);
                    variant.addWeapon(slotId, weaponId);
                    weaponGroupSpec.addSlot(slotId);
                }
                weaponGroupSpec.setAutofireOnByDefault(autofire);
                weaponGroupSpec.setType(WeaponGroupType.valueOf(mode));
                variant.addWeaponGroup(weaponGroupSpec);
            }
            if (wings != null) {
                int numBuiltIn = Global.getSettings().getVariant(variant.getHullSpec().getHullId() + "_Hull").getFittedWings().size();
                for (int w = 0; w < wings.length(); w++) {
                    variant.setWingId(numBuiltIn + w, wings.getString(w));
                }
            }
        } catch (Exception e) {
            log.warn("could not load ship variant at " + path, e);
        }

        //Maintain the S-mods through salvage
        if (variant != null) {
            variant.addTag(Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE);
        }

        return variant;
    }

    /**
     * Spawn a fleet in its intended location with the proper order and target
     *
     * @param fleet
     * @param target
     * @param isImportant
     * @param transponderOn
     * @param verbose
     * @nullable @param spawnLocation
     * @nullable @param assignment
     */
    public static void spawnFleet(
            CampaignFleetAPI fleet,
            @Nullable SectorEntityToken spawnLocation,
            @Nullable FleetAssignment assignment,
            SectorEntityToken target,
            boolean isImportant,
            boolean transponderOn,
            boolean verbose
    ) {

        //defaults
        FleetAssignment order = FleetAssignment.ORBIT_AGGRESSIVE;
        if (assignment != null) {
            order = assignment;
        }
        SectorEntityToken location = target;
        if (spawnLocation != null) {
            location = spawnLocation;
        }

        //spawn placement and assignement
        LocationAPI systemLocation = location.getContainingLocation();
        systemLocation.addEntity(fleet);
        fleet.setLocation(location.getLocation().x, location.getLocation().y);
        if (order == FleetAssignment.PATROL_SYSTEM) {
            fleet.addAssignment(order, target.getStarSystem().getStar(), 1000000f);
        } else {
            fleet.addAssignment(order, target, 1000000f);
        }
        //radius fix
        fleet.forceSync();
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();

        //ancillary stuff
        fleet.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, isImportant);
        fleet.setTransponderOn(transponderOn);

        if (verbose) {
            log.info("Spawned " + fleet.getName() + " around " + location.getId() + " in the " + location.getStarSystem().getId() + " system.");
            log.info("Order: " + order.name() + ", target: " + target.getId() + " in the " + target.getStarSystem().getId() + " system.");
        }
    }


    /////////////////////////
    //                     //
    //  STAR SYSTEM STUFF  //
    //                     //
    /////////////////////////


    /**
     * Removes hyperspace clouds around the system, up to the outer-most jump point radius
     *
     * @param system StarSystemAPI that needs cleanup
     */
    public static void hyperspaceCleanup(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }

    /**
     * Place an object on a stable orbit similar to the most approaching existing one that can be found
     *
     * @param object
     * @param spin
     */
    public static void placeOnStableOrbit(SectorEntityToken object, boolean spin) {
        //prevent crash in hyperspace
        if (object.isInHyperspace()) {
            object.setFixedLocation(object.getLocation().x, object.getLocation().y);
            object.setFacing(MathUtils.getRandomNumberInRange(0, 360));
            return;
        }

        StarSystemAPI system = object.getStarSystem();
        Vector2f location = object.getLocation();

        //find a reference for the orbit
        SectorEntityToken referenceObject = null;
        float closestOrbit = 999999999;
        //find nearest orbit to match
        for (SectorEntityToken e : system.getAllEntities()) {

            //skip self
            if (e == object) continue;
            //skip stars
            if (e.isStar()) continue;
            //skip non orpiting objects
            if (e.getOrbit() == null || e.getOrbitFocus() == null || e.getCircularOrbitRadius() <= 0 || e.getCircularOrbitPeriod() <= 0)
                continue;

            //find closest point on orbit for the tested object
            Vector2f closestPointOnOrbit = MathUtils.getPoint(
                    e.getOrbitFocus().getLocation(),
                    e.getCircularOrbitRadius(),
                    VectorUtils.getAngle(e.getOrbitFocus().getLocation(), location)
            );

            //closest orbit becomes the reference
            if (MathUtils.getDistanceSquared(closestPointOnOrbit, location) < closestOrbit) {
                referenceObject = e;
                closestOrbit = MathUtils.getDistanceSquared(closestPointOnOrbit, location);
            }
        }

        SectorEntityToken orbitCenter;
        Float angle, radius, period;

        if (referenceObject != null) {
            orbitCenter = referenceObject.getOrbitFocus();
            angle = VectorUtils.getAngle(referenceObject.getOrbitFocus().getLocation(), location);
            radius = MathUtils.getDistance(referenceObject.getOrbitFocus().getLocation(), location);
            period = referenceObject.getCircularOrbitPeriod() * (MathUtils.getDistance(referenceObject.getOrbitFocus().getLocation(), location) / referenceObject.getCircularOrbitRadius());
        } else {
            orbitCenter = system.getCenter();
            angle = VectorUtils.getAngle(system.getCenter().getLocation(), location);
            radius = MathUtils.getDistance(system.getCenter(), location);
            period = MathUtils.getDistance(system.getCenter(), location) / 2;
        }
        if (spin) {
            object.setCircularOrbitWithSpin(orbitCenter, angle, radius, period, -10, 10);
        } else {
            object.setCircularOrbit(orbitCenter, angle, radius, period);
        }
    }

    /**
     * Creates a derelict ship at the desired emplacement.
     *
     * @param variantId       Spawned ship variant
     * @param condition       Condition of the derelict. Better conditions means fewer D-mods but also more weapons from the variant
     * @param discoverable    Awards XP when found
     * @param discoveryXp     XP awarded when found (<0 to use the default)
     * @param recoverable     Can be salvaged as long as the hull it's possible for the player to salvage this hull
     *                        (eg Automated ships still require the skill).
     * @param orbitCenter     Entity orbited
     * @param orbitStartAngle Orbit starting angle
     * @param orbitRadius     Orbit radius
     * @param orbitDays       Orbit period
     * @return
     */
    public static SectorEntityToken createDerelict(
            String variantId,
            ShipRecoverySpecial.ShipCondition condition,
            boolean discoverable,
            Integer discoveryXp,
            boolean recoverable,
            SectorEntityToken orbitCenter,
            float orbitStartAngle,
            float orbitRadius,
            float orbitDays
    ) {
        ShipRecoverySpecial.PerShipData shipData = new ShipRecoverySpecial.PerShipData(variantId, condition);
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(shipData, false);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(orbitCenter.getStarSystem(), Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(discoverable);
        if (discoveryXp != null && discoveryXp >= 0) {
            ship.setDiscoveryXP((float) discoveryXp);
        }

        ship.setCircularOrbit(orbitCenter, orbitStartAngle, orbitRadius, orbitDays);

        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        }

        return ship;
    }

    /**
     * Creates a debris field with generic commodity loot to salvage
     *
     * @param id                  field ID
     * @param radius              field radius in su (clamped to 1000)
     * @param density             field visual density
     * @param duration            field duration in days (set to a negative value for a permanent field)
     * @param glowDuration        time in days with glowing debris
     * @param salvageXp           XP awarded for salvaging (<0 to use the default)
     * @param defenderProbability chance of an enemy fleet guarding the debris field (<0 to ignore)
     * @param defenderFaction     defender's faction
     * @param defenderFP          defender fleet's size in Fleet Points
     * @param detectionMult       detection distance multiplier (<0 to use the default)
     * @param discoverable        awards XP when found
     * @param discoveryXp         XP awarded when found (<0 to use the default)
     * @param orbitCenter         entity orbited
     * @param orbitStartAngle     orbit starting angle
     * @param orbitRadius         orbit radius
     * @param orbitDays           orbit period
     * @return
     */
    public static SectorEntityToken createDebrisField(
            String id,
            float radius,
            float density,
            float duration,
            float glowDuration,
            @Nullable Integer salvageXp,
            float defenderProbability,
            @Nullable String defenderFaction,
            @Nullable Integer defenderFP,
            float detectionMult,
            boolean discoverable,
            @Nullable Integer discoveryXp,
            SectorEntityToken orbitCenter,
            float orbitStartAngle,
            float orbitRadius,
            float orbitDays
    ) {

        float theDuration;
        if (duration <= 0) {
            theDuration = 999999;
        } else {
            theDuration = duration;
        }
        float theGlowDuration;
        if (glowDuration < 0) {
            theGlowDuration = 0;
        } else {
            theGlowDuration = glowDuration;
        }

        DebrisFieldTerrainPlugin.DebrisFieldParams params = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                Math.min(radius, 1000), // field radius - should not go above 1000 for performance reasons
                density, // density, visual - affects number of debris pieces
                theDuration, // duration in days
                theGlowDuration); // days the field will keep generating glowing pieces
        params.source = DebrisFieldSource.MIXED;
        params.baseDensity = density;
        if (salvageXp != null && salvageXp >= 0) {
            params.baseSalvageXP = salvageXp; // base XP for scavenging in field
        }
        if (defenderProbability > 0 && defenderFaction != null && defenderFP != null && defenderFP > 0) {
            params.defFaction = defenderFaction;
            params.defenderProb = defenderProbability;
            params.maxDefenderSize = defenderFP;
        }
        SectorEntityToken generatedDebris = Misc.addDebrisField(orbitCenter.getStarSystem(), params, StarSystemGenerator.random);
        generatedDebris.setId(id);
        if (detectionMult > 0) {
            generatedDebris.setSensorProfile(detectionMult);
        }
        generatedDebris.setDiscoverable(discoverable);
        if (discoveryXp != null && discoveryXp >= 0) {
            generatedDebris.setDiscoveryXP((float) discoveryXp);
        }
        generatedDebris.setCircularOrbit(orbitCenter, orbitStartAngle, orbitRadius, orbitDays);

        return generatedDebris;
    }

    public static enum lootType {
        SUPPLIES,
        FUEL,
        CREW,
        MARINES,
        COMMODITY,
        WEAPON,
        FIGHTER,
        HULLMOD,
        SPECIAL,
    }

    /**
     * Adds items to a salvageable entity such as a debris field, a recoverable ship or a wreck.
     *
     * @param cargo   cargo to add salvage to, creates a new cargo if NULL
     * @param carrier entity with the loot
     * @param type    MagicSystem.lootType, type of loot added,
     * @param lootID  specific ID of the loot found, NULL for supplies, fuel, crew, marines,   trade commodities can use campaign.ids.Commodities
     * @param amount
     * @return The resulting cargo you can still add to.
     */
    public static CargoAPI addSalvage(@Nullable CargoAPI cargo, SectorEntityToken carrier, lootType type, @Nullable String lootID, int amount) {
        CargoAPI theCargo = Global.getFactory().createCargo(true);
        if (cargo != null) {
            theCargo = cargo;
        }
        switch (type) {
            case SPECIAL:
                theCargo.addItems(CargoAPI.CargoItemType.SPECIAL, lootID, amount);
                break;
            case COMMODITY:
                theCargo.addCommodity(lootID, amount);
                break;
            case HULLMOD:
                theCargo.addHullmods(lootID, amount);
                break;
            case WEAPON:
                theCargo.addWeapons(lootID, amount);
                break;
            case FIGHTER:
                theCargo.addFighters(lootID, amount);
                break;
            case SUPPLIES:
                theCargo.addSupplies(amount);
                break;
            case FUEL:
                theCargo.addFuel(amount);
                break;
            case CREW:
                theCargo.addCrew(amount);
                break;
            case MARINES:
                theCargo.addMarines(amount);
                break;
        }
        BaseSalvageSpecial.addExtraSalvage(theCargo, carrier.getMemoryWithoutUpdate(), -1);
        return theCargo;
    }

    /**
     * Shorthand to add custom jump-point.
     * Make sure to call `system.autogenerateHyperspaceJumpPoints` after this to generate the hyperspace side.
     *
     * @param id              jump point's internal ID
     * @param name            jump point's displayed name
     * @param linkedPlanet    planet displayed from hyperspace, can be null
     * @param orbitCenter     entity orbited
     * @param orbitStartAngle orbit starting angle
     * @param orbitRadius     orbit radius
     * @param orbitDays       orbit period
     * @since 0.46.0
     */
    public static SectorEntityToken addJumpPoint(
            @NotNull String id,
            @NotNull String name,
            @Nullable SectorEntityToken linkedPlanet,
            @NotNull SectorEntityToken orbitCenter,
            float orbitStartAngle,
            float orbitRadius,
            float orbitDays
    ) {
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(id, name);
        if (linkedPlanet != null) {
            jumpPoint.setRelatedPlanet(linkedPlanet);
        }
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        jumpPoint.setCircularOrbit(orbitCenter, orbitStartAngle, orbitRadius, orbitDays);
        orbitCenter.getStarSystem().addEntity(jumpPoint);

        return jumpPoint;
    }

    /**
     * Adds a simple custom market to a system entity
     *
     * @param entity
     * @param id
     * @param name
     * @param size
     * @param faction
     * @param isFreeport
     * @param isHidden
     * @param conditions         list of conditions from campaign.ids.Conditions
     * @param industries         list of industries from campaign.ids.Industries
     * @param hasStorage
     * @param paidForStorage     is storage already paid for
     * @param hasBlackmarket
     * @param hasOpenmarket
     * @param hasMilitarymarket
     * @param isAbandonedStation
     * @return
     */
    public static MarketAPI addSimpleMarket(
            SectorEntityToken entity,
            String id,
            String name,
            Integer size,
            String faction,
            boolean isFreeport,
            boolean isHidden,
            List<String> conditions,
            List<String> industries,
            boolean hasStorage,
            boolean paidForStorage,
            boolean hasBlackmarket,
            boolean hasOpenmarket,
            boolean hasMilitarymarket,
            boolean isAbandonedStation
    ) {

        MarketAPI market = Global.getFactory().createMarket(id, name, size);
        market.setPrimaryEntity(entity);
        market.setFactionId(faction);
        market.setFreePort(isFreeport);
        market.setHidden(isHidden);

        //add conditions and industries
        if (!conditions.isEmpty()) {
            for (String c : conditions) {
                market.addCondition(c);
            }
        }
        if (!industries.isEmpty()) {
            for (String i : industries) {
                market.addIndustry(i);
            }
        }

        //add submarkets
        if (hasStorage) {
            market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
            if (paidForStorage) {
                ((StoragePlugin) market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()).setPlayerPaidToUnlock(true);
            }
        }
        if (hasBlackmarket) {
            market.addSubmarket(Submarkets.SUBMARKET_BLACK);
        }
        if (hasOpenmarket) {
            market.addSubmarket(Submarkets.SUBMARKET_OPEN);
        }
        if (hasMilitarymarket) {
            market.addSubmarket(Submarkets.GENERIC_MILITARY);
        }

        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        if (isAbandonedStation) {
            entity.getMemoryWithoutUpdate().set("$abandonedStation", true);
        }
        entity.setMarket(market);

        return market;
    }

    /**
     * Adds a custom character to a given market
     *
     * @param market                   MarketAPI the person is added to
     * @param firstName                person's first name
     * @param lastName                 person's last name
     * @param portraitId               id of the sprite in settings.json/graphics/characters
     * @param gender                   FullName.Gender
     * @param factionId                person's faction
     * @param rankId                   rank from campaign.ids.Ranks
     * @param postId                   post from campaign.ids.Ranks
     * @param isMarketAdmin
     * @param industrialPlanning_level skill level for market admin
     * @param spaceOperation_level     skill level for market admin
     * @param groundOperations_level   skill level for market admin
     * @param commScreenPosition       position order in the comm screen, 0 is the admin position
     * @return
     */
    public static PersonAPI addCustomPerson(
            MarketAPI market,
            String firstName,
            String lastName,
            String portraitId,
            FullName.Gender gender,
            String factionId,
            String rankId,
            String postId,
            boolean isMarketAdmin,
            Integer industrialPlanning_level,
            Integer commScreenPosition
    ) {

        PersonAPI person = Global.getFactory().createPerson();
        person.getName().setFirst(firstName);
        person.getName().setLast(lastName);
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", portraitId));
        person.setGender(gender);
        person.setFaction(factionId);

        person.setRankId(rankId);
        person.setPostId(postId);

        person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, industrialPlanning_level);
//        person.getStats().setSkillLevel(Skills.SPACE_OPERATIONS, spaceOperation_level);
//        person.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, groundOperations_level);

        if (isMarketAdmin) {
            market.setAdmin(person);
        }
        market.getCommDirectory().addPerson(person, commScreenPosition);
        market.addPerson(person);

        return person;
    }


    //BOUNTY CHECKS THAT MAY PROVE USEFULL FOR OTHER THINGS:


    /**
     * @param market                   checked market for triggers
     * @param market_id                list of preferred market IDs, bypass all other checks
     * @param marketFaction_any        list of suitable faction IDs, any will do
     * @param marketFaction_alliedWith market is suitable if least FAVORABLE with ANY of the required factions
     * @param marketFaction_none       list of unsuitable faction IDs
     * @param marketFaction_enemyWith  market is unsuitable if it is not HOSTILE with EVERY blacklisted factions
     * @param market_minSize           minimal market size
     * @return
     */
    public static boolean isAvailableAtMarket(
            MarketAPI market,
            @Nullable List<String> market_id,
            @Nullable List<String> marketFaction_any,
            boolean marketFaction_alliedWith,
            @Nullable List<String> marketFaction_none,
            boolean marketFaction_enemyWith,
            int market_minSize
    ) {

        List<String> marketBlacklist = MagicSettings.getList(MagicVariables.MAGICLIB_ID, "bounty_market_blacklist");

        if (marketBlacklist.contains(market.getId())) {
            return false;
        }

        //exact id match beats everything
        if (market_id != null && !market_id.isEmpty()) {
            for (String m : market_id) {
                if (market.getId().equals(m)) return true;
            }

            for (String id : market_id) {
                //if at least one of the priority market exists, stop here as the bounty shall only be offered there
                if (Global.getSector().getEntityById(id) != null) {
                    return false;
                }
            }
        }

        //checking trigger_market_minSize
        if (market_minSize > 0 && market.getSize() < market_minSize) return false;

        //checking trigger_marketFaction_none and trigger_marketFaction_enemyWith
        if (marketFaction_none != null && !marketFaction_none.isEmpty()) {
            for (String f : marketFaction_none) {
                //skip non existing factions
                if (Global.getSector().getFaction(f) == null) {
                    if (MagicVariables.verbose) {
                        log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    }
                    continue;
                }

                FactionAPI this_faction = Global.getSector().getFaction(f);
                if (market.getFaction() == this_faction) {
                    return false; //is one of the excluded factions
                } else if (marketFaction_enemyWith && market.getFaction().isAtBest(this_faction, RepLevel.HOSTILE)) {
                    return true; //is hostile with one of the excluded factions
                }
            }
        }

        //checking trigger_marketFaction_any and trigger_marketFaction_alliedWith
        if (marketFaction_any != null && !marketFaction_any.isEmpty()) {

            for (String f : marketFaction_any) {
                //skip non existing factions
                if (Global.getSector().getFaction(f) == null) {
                    if (MagicVariables.verbose) {
                        log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    }
                    continue;
                }

                FactionAPI this_faction = Global.getSector().getFaction(f);
                if (market.getFaction() == this_faction) {
                    return true; //is one of the required factions
                } else if (marketFaction_alliedWith && market.getFaction().isAtWorst(this_faction, RepLevel.WELCOMING)) {
                    return true;  //is friendly toward one of the required factions
                }
            }
            return false; //the loop has not exited earlier, therefore it failed all of the market faction checks
        }

        //failed none of the tests, must be good then
        return true;
    }

    public static boolean isAvailableToPlayer(
            int player_minLevel,
            int min_days_elapsed,
            int min_fleet_size,
            @Nullable Map<String, Boolean> memKeys_all,
            @Nullable Map<String, Boolean> memKeys_any,
            @Nullable Map<String, Boolean> memKeys_none,
            @Nullable Map<String, Float> playerRelationship_atLeast,
            @Nullable Map<String, Float> playerRelationship_atMost
    ) {

        //checking trigger_min_days_elapsed
//        if(min_days_elapsed>0 && Global.getSector().getClock().getDay()<min_days_elapsed)return false;
        if (min_days_elapsed > 0 && Global.getSector().getClock().getDay() + (Global.getSector().getClock().getCycle() - 206) * 365 < min_days_elapsed)
            return false;

        //checking trigger_player_minLevel
        if (player_minLevel > 0 && Global.getSector().getPlayerStats().getLevel() < player_minLevel) return false;

        //checking trigger_min_fleet_size
        if (min_fleet_size > 0) {
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
//            float effectiveFP = playerFleet.getEffectiveStrength();
            float effectiveFP = playerFleet.getFleetPoints();
            if (min_fleet_size > effectiveFP) {
                if (MagicVariables.verbose) {
                    log.info(String.format("Requirement not met: min fleet size of %s requested, currently %s.", min_fleet_size, effectiveFP));
                }
                return false;
            }
        }

        //checking trigger_playerRelationship_atLeast
        boolean relation = false;
        if (playerRelationship_atLeast != null && !playerRelationship_atLeast.isEmpty()) {
            for (String f : playerRelationship_atLeast.keySet()) {
                //skip non existing factions
                if (Global.getSector().getFaction(f) == null) {
                    if (MagicVariables.verbose) {
                        log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    }
                    continue;
                }
                if (Global.getSector().getPlayerFaction().isAtWorst(f, RepLevel.getLevelFor(playerRelationship_atLeast.get(f))))
                    relation = true;
            }
        } else {
            relation = true;
        }

        //checking trigger_playerRelationship_atMost
        boolean hostility = false;
        if (playerRelationship_atMost != null && !playerRelationship_atMost.isEmpty()) {
            for (String f : playerRelationship_atMost.keySet()) {
                //skip non existing factions
                if (Global.getSector().getFaction(f) == null) {
                    if (MagicVariables.verbose) {
                        log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    }
                    continue;
                }
                if (Global.getSector().getPlayerFaction().isAtBest(f, RepLevel.getLevelFor(playerRelationship_atMost.get(f))))
                    hostility = true;
            }
        } else {
            hostility = true;
        }

        if (!relation || !hostility) {
            if (MagicVariables.verbose) {
                if (!relation && playerRelationship_atLeast != null && !playerRelationship_atLeast.isEmpty())
                    log.info(String.format("Requirement not met: Relationship too low with %s ", playerRelationship_atLeast.keySet()));
                if (!hostility && playerRelationship_atMost != null && !playerRelationship_atMost.isEmpty())
                    log.info(String.format("Requirement not met: Relationship too high with %s ", playerRelationship_atMost.keySet()));
            }
            return false;
        }

        //checking trigger_memKeys_all
        if (memKeys_all != null && !memKeys_all.isEmpty()) {
            for (String f : memKeys_all.keySet()) {
                //check if the memKey exists 
                if (!Global.getSector().getMemoryWithoutUpdate().getKeys().contains(f)) {
                    if (MagicVariables.verbose) {
                        log.info(String.format("Requirement not met: memKeys_all %s key not fount.", f));
                    }
                    return false;
                }
                //check if it has the proper value
                if (memKeys_all.get(f) != Global.getSector().getMemoryWithoutUpdate().getBoolean(f)) {
                    if (MagicVariables.verbose) {
                        log.info(String.format("Requirement not met: memKeys_all %s key is not %s.", f, memKeys_all.get(f)));
                    }
                    return false;
                }
            }
        }

        //checking memKeys_none
        if (memKeys_none != null && !memKeys_none.isEmpty()) {
            for (Map.Entry<String, Boolean> entry : memKeys_none.entrySet()) {
                if (Global.getSector().getMemoryWithoutUpdate().contains(entry.getKey())) {
                    if (Global.getSector().getMemoryWithoutUpdate().getBoolean(entry.getKey()) == entry.getValue()) {
                        if (MagicVariables.verbose) {
                            log.info(String.format("Requirement not met: memKeys_none %s value %s is present.", entry.getKey(), entry.getValue()));
                        }
                        return false;
                    }
                }
            }
        }

        //checking trigger_memKeys_any
        if (memKeys_any != null && !memKeys_any.isEmpty()) {
            for (String f : memKeys_any.keySet()) {
                //check if the memKey exists 
                if (!Global.getSector().getMemoryWithoutUpdate().getKeys().contains(f)) {
                    //check if it has the proper value
                    if (memKeys_any.get(f) == Global.getSector().getMemoryWithoutUpdate().getBoolean(f)) {
                        return true;
                    }
                }
            }
            //the loop has not been exited therefore some key is missing
            if (MagicVariables.verbose) {
                log.info(String.format("Requirement not met: none of the memKeys_any is present with the proper value: %s ", memKeys_any.keySet()));
            }
            return false;
        }

        //failed none of the tests, must be good then
        return true;
    }

    public static Float PlayerFleetSizeMultiplier(float enemyBaseFP) { //base FP is the min FP of the enemy fleet with reinforcements.
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
//        float effectiveFP = playerFleet.getEffectiveStrength();
        float effectiveFP = playerFleet.getFleetPoints();
        return effectiveFP / enemyBaseFP;
    }

    public static Float RelativeEffectiveStrength(CampaignFleetAPI enemyFleet) {

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        if (enemyFleet == null || enemyFleet.getFleetData() == null || playerFleet == null || playerFleet.getFleetData() == null)
            return null;

        float playerEffectiveStrength = 0f;
        float enemyEffectiveStrength = 0f;

        if (MagicVariables.verbose) {
            log.info("\n");
            log.info("PLAYER strength");
        }

        for (FleetMemberAPI m : playerFleet.getFleetData().getMembersListCopy()) {
            float strength = EffectiveFleetMemberStrength(m);
            if (MagicVariables.verbose) {
                log.info(m.getHullId() + " strength = " + strength);
            }
            playerEffectiveStrength += strength;
        }

        if (MagicVariables.verbose) {
            log.info("Effective player strength = " + playerEffectiveStrength + "\n");
            log.info("ENEMY strength");
        }

        for (FleetMemberAPI m : enemyFleet.getFleetData().getMembersListCopy()) {
            float strength = EffectiveFleetMemberStrength(m);
            if (MagicVariables.verbose) {
                log.info(m.getHullId() + " strength = " + strength);
            }
            enemyEffectiveStrength += strength;
        }

        float relativeStrength = playerEffectiveStrength / enemyEffectiveStrength;

        if (MagicVariables.verbose) {
            log.info("Effective enemy strength = " + enemyEffectiveStrength + "\n");
            log.info("Relative strength = " + relativeStrength);
        }

        return relativeStrength;
    }

    private static float EffectiveFleetMemberStrength(FleetMemberAPI member) {

        float str = Math.max(1f, member.getMemberStrength());

        float quality = 0.8f;

        if (member.getFleetData() != null && member.getFleetData().getFleet() != null) {
            if (member.getFleetData().getFleet().getInflater() != null && !member.getFleetData().getFleet().isInflated()) {
                quality = 1 + (member.getFleetData().getFleet().getInflater().getQuality() - 1) / 2;
            } else {
                float dmods = DModManager.getNumDMods(member.getVariant());
                quality = Math.max(0.25f, 1f - 0.1f * dmods);
            }
        }

        if (member.isStation()) quality = 1f;

        float damageMult = 0.5f + 0.5f * member.getStatus().getHullFraction();

        float captainMult = 1f;
        if (member.getCaptain() != null) {
            float captainLevel = (member.getCaptain().getStats().getLevel() - 1f);
            if (member.isStation())
                captainMult += captainLevel / (MAX_OFFICER_LEVEL * 3f);
            else
                captainMult += captainLevel / (MAX_OFFICER_LEVEL * 2f);
        }


        if (Global.getSector().getPlayerFleet().getFlagship() == member) {
            //PLAYER multiplier
            captainMult = Global.getSector().getPlayerStats().getLevel();
            captainMult /= 20;
            captainMult += 1;
        }


        str *= quality;
        str *= damageMult;
        str *= captainMult;

//        if(member.isCivilian())str*=0.5f;

        return str;
    }
    /*
    @Nullable
    public static SectorEntityToken findSuitableTarget(
            @Nullable List<String> entityIDs,
            @Nullable List<String> marketFactions,
            @Nullable String distance,
            @Nullable List<String> seek_themes,
            @Nullable List<String> avoid_themes,
            @Nullable List<String> entities,
            boolean defaultToAnyEntity,
            boolean prioritizeUnexplored,
            boolean verbose){
        return findSuitableTarget(entityIDs,marketFactions,distance,seek_themes,avoid_themes,entities,false,defaultToAnyEntity,prioritizeUnexplored,verbose);
    }
    */

    /**
     * Returns a random target SectorEntityToken given the following parameters:
     *
     * @param entityIDs            List of IDs of preferred markets to target, supercedes all,              default to other parameters if none of those markets exist
     * @param marketFactions       List of faction to pick a market from, supercedes all but market ids,    default to other parameters if none of those markets exist
     * @param distance             "CORE", "CLOSE", "FAR", preferred range band for the target system if any
     * @param seek_themes          List of preferred system themes TAGS from campaign.ids.Tags,             plus "PROCGEN_NO_THEME" and "PROCGEN_NO_THEME_NO_PULSAR_NO_BLACKHOLE"
     * @param avoid_themes         List of blacklisted system themes
     * @param entities             List of preferred entity types from campaign.ids.Tags
     * @param defaultToAnyEntity   If none of the systems in the required range band has any of the required entities, will the script default to any entity within a system with the proper range and themes instead of looking into a different range band
     * @param prioritizeUnexplored Will the script target unexplored systems first before falling back to ones that have been visited by the player
     * @param verbose              Log some debug messages
     * @return
     */
    @Nullable
    public static SectorEntityToken findSuitableTarget(
            @Nullable List<String> entityIDs,
            @Nullable List<String> marketFactions,
            @Nullable String distance,
            @Nullable List<String> seek_themes,
            @Nullable List<String> avoid_themes,
            @Nullable List<String> entities,
            //boolean defaultToAnySystem,
            boolean defaultToAnyEntity,
            boolean prioritizeUnexplored,
            boolean verbose) {

        if (verbose) {
            log.info("Find Suitable Target log");
            log.info("Checking marketIDs");
        }
        //first priority, check if the preset location(s) exist(s)
        if (entityIDs != null && !entityIDs.isEmpty()) {
            //if there is just one location and it exist, lets use that.
            if (entityIDs.size() == 1 && Global.getSector().getEntityById(entityIDs.get(0)) != null) {
                if (verbose) {
                    SectorEntityToken t = Global.getSector().getEntityById(entityIDs.get(0));
                    log.info("Selecting " + t.getName() + ", in the " + t.getContainingLocation().getName() + " system, " + t.getContainingLocation().getLocation().length() + " (" + Misc.getDistanceLY(new Vector2f(), t.getContainingLocation().getLocation()) + " LY) from the sector's center");
                }
                return Global.getSector().getEntityById(entityIDs.get(0));
            }
            //if there are multiple possible location, pick a random one
            WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
            for (String loc : entityIDs) {
                if (Global.getSector().getEntityById(loc) != null) picker.add(Global.getSector().getEntityById(loc));
            }

            if (verbose) {
                log.info("There are " + picker.getTotal() + " available market ids for pick");
            }

            if (!picker.isEmpty()) {
                if (verbose) {
                    SectorEntityToken picked = picker.pick();
                    log.info("Selecting " + picked.getName() + ", in the " + picked.getContainingLocation().getName() + " system, " + picked.getContainingLocation().getLocation().length() + " (" + Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) + " LY) from the sector's center");
                    return picked;
                } else return picker.pick();
            }
        }


        if (verbose) {
            log.info("Checking market factions");
        }
        //second priority is faction markets
        if (marketFactions != null && !marketFactions.isEmpty()) {
            WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
            for (MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
                if (marketFactions.contains(m.getFaction().getId()) && !picker.getItems().contains(m.getPrimaryEntity())) {
                    picker.add(m.getPrimaryEntity());
                }
            }

            if (verbose) {
                log.info("There are " + picker.getTotal() + " available faction markets for pick");
            }

            if (!picker.isEmpty()) {
                if (verbose) {
                    SectorEntityToken picked = picker.pick();
                    log.info("Selecting " + picked.getName() + ", in the " + picked.getContainingLocation().getName() + " system, " + picked.getContainingLocation().getLocation().length() + " (" + Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) + " LY) from the sector's center");
                    return picked;
                } else return picker.pick();
            }
        }

        if (
                (distance == null || distance.equals("")) &&
                        (seek_themes == null || seek_themes.isEmpty()) &&
                        (entities == null || entities.isEmpty())
        ) {
            //there was no fallback filters defined, this is a wrap
            return null;
        }

        //time for some pain

        //calculate the sector size to define range bands
        float sector_width = MagicVariables.getSectorSize();

        if (verbose) {
            log.info("Checking preferences");
            log.info("Sector width = " + sector_width);
        }
        if (verbose) {
            log.info("Finding system with required themes");
        }
        //start with the themes preferences
        List<StarSystemAPI> systems_core = new ArrayList<>();
        List<StarSystemAPI> systems_close = new ArrayList<>();
        List<StarSystemAPI> systems_far = new ArrayList<>();
        if (seek_themes != null && !seek_themes.isEmpty()) {
            for (StarSystemAPI s : Global.getSector().getStarSystems()) {
                for (String this_theme : seek_themes) {
                    if (s.hasTag(this_theme)) {
                        //sort systems by distances because that will come in handy later
                        float dist = s.getLocation().length();
                        if (dist < sector_width * 0.33f) {
                            systems_core.add(s);
                        } else if (dist < sector_width * 0.66f) {
                            systems_close.add(s);
                        } else {
                            systems_far.add(s);
                        }
                        break;
                    }
                }
                //special test for basic procgen systems without special content
                if (seek_themes.contains(MagicVariables.SEEK_EMPTY_SYSTEM) || seek_themes.contains(MagicVariables.SEEK_EMPTY_SAFE_SYSTEM)) {
                    if (s.isProcgen()) {
                        if (seek_themes.contains(MagicVariables.SEEK_EMPTY_SAFE_SYSTEM) && (s.hasBlackHole() || s.hasPulsar()))
                            continue;
                        //check for the 3 bland themes
                        if (s.getTags().contains("theme_misc_skip") || s.getTags().contains("theme_misc") || s.getTags().contains("theme_core_unpopulated")) {
                            //sort systems by distances because that will come in handy later
                            float dist = s.getLocation().length();
                            if (dist < sector_width * 0.33f) {
                                systems_core.add(s);
                            } else if (dist < sector_width * 0.66f) {
                                systems_close.add(s);
                            } else {
                                systems_far.add(s);
                            }
                        }
                    }
                }
            }
        } else {
            //if there isn't any THEME preference, let's add *everything*
            for (StarSystemAPI s : Global.getSector().getStarSystems()) {
                //sort systems by distances because that will come in handy later
                float dist = s.getLocation().length();
                if (dist < sector_width * 0.33f) {
                    systems_core.add(s);
                } else if (dist < sector_width * 0.66f) {
                    systems_close.add(s);
                } else {
                    systems_far.add(s);
                }
            }
        }

        //if the lists are empty but fallback is on, add everything
        if (systems_core.isEmpty() && systems_close.isEmpty() && systems_far.isEmpty()) {
            if (defaultToAnyEntity) {
                for (StarSystemAPI s : Global.getSector().getStarSystems()) {
                    //sort systems by distances because that will come in handy later
                    float dist = s.getLocation().length();
                    if (dist < sector_width * 0.33f) {
                        systems_core.add(s);
                    } else if (dist < sector_width * 0.66f) {
                        systems_close.add(s);
                    } else {
                        systems_far.add(s);
                    }
                }
            } else {
                //all lists are empty, no fallback option for systems
                if (verbose) {
                    log.warn("No valid system theme found");
                }
                return null;
            }
        }

        //cull systems with blacklisted themes
        if (avoid_themes != null && !avoid_themes.isEmpty()) {

            //merge default theme blacklist if needed 
            if (avoid_themes.contains(MagicVariables.AVOID_OCCUPIED_SYSTEM)) {
                for (String s : MagicVariables.mergedThemesBlacklist) {
                    if (!avoid_themes.contains(s)) avoid_themes.add(s);
                }
            }

            boolean noPBH = false;
            if (avoid_themes.contains(MagicVariables.AVOID_BLACKHOLE_PULSAR)) noPBH = true;
            boolean noPop = false;
            if (avoid_themes.contains(MagicVariables.AVOID_OCCUPIED_SYSTEM)) noPop = true;

            if (!systems_core.isEmpty()) {
                for (int i = 0; i < systems_core.size(); i++) {
                    for (String t : avoid_themes) {
                        if (noPBH && (systems_core.get(i).hasBlackHole() || systems_core.get(i).hasPulsar())) {
                            systems_core.remove(i);
                            i--;
                            break;
                        } else
                            //manually check for markets
                            if (noPop && !Global.getSector().getEconomy().getMarkets(systems_core.get(i)).isEmpty()) {
                                systems_core.remove(i);
                                i--;
                                break;
                            } else
                                // check for blacklisted theme
                                if (systems_core.get(i).getTags().contains(t)) {
                                    systems_core.remove(i);
                                    i--;
                                    break;
                                }
                    }
                }
            }
            if (!systems_close.isEmpty()) {
                for (int i = 0; i < systems_close.size(); i++) {
                    for (String t : avoid_themes) {
                        if (noPBH && (systems_close.get(i).hasBlackHole() || systems_close.get(i).hasPulsar())) {
                            systems_close.remove(i);
                            i--;
                            break;
                        } else
                            //manually check for markets
                            if (noPop && !Global.getSector().getEconomy().getMarkets(systems_close.get(i)).isEmpty()) {
                                systems_close.remove(i);
                                i--;
                                break;
                            } else
                                // check for blacklisted theme
                                if (systems_close.get(i).getTags().contains(t)) {
                                    systems_close.remove(i);
                                    i--;
                                    break;
                                }
                    }
                }
            }
            if (!systems_far.isEmpty()) {
                for (int i = 0; i < systems_far.size(); i++) {
                    for (String t : avoid_themes) {
                        if (noPBH && (systems_far.get(i).hasBlackHole() || systems_far.get(i).hasPulsar())) {
                            systems_far.remove(i);
                            i--;
                            break;
                        } else
                            //manually check for markets
                            if (noPop && !Global.getSector().getEconomy().getMarkets(systems_far.get(i)).isEmpty()) {
                                systems_far.remove(i);
                                i--;
                                break;
                            } else
                                // check for blacklisted theme
                                if (systems_far.get(i).getTags().contains(t)) {
                                    systems_far.remove(i);
                                    i--;
                                    break;
                                }
                    }
                }
            }
        }

        if (verbose) {
            log.info("There are " + systems_core.size() + " themed systems in the core");
            log.info("There are " + systems_close.size() + " themed systems close to the core");
            log.info("There are " + systems_far.size() + " themed systems far from the core");
        }

        //now order the selected system lists by distance preferences
        List<List<StarSystemAPI>> distance_priority = new ArrayList<>();
        if (distance == null || distance.equals("")) {
            //random distance 
            distance_priority.add(0, systems_core);
            distance_priority.add(1, systems_close);
            distance_priority.add(2, systems_far);
            Collections.shuffle(distance_priority);
        } else switch (distance) {
            case "CORE": {
                distance_priority.add(0, systems_core);
                distance_priority.add(1, systems_close);
                distance_priority.add(2, systems_far);
                break;
            }
            case "CLOSE": {
                distance_priority.add(0, systems_close);
                distance_priority.add(1, systems_far);
                distance_priority.add(2, systems_core);
                break;
            }
            case "FAR": {
                distance_priority.add(0, systems_far);
                distance_priority.add(1, systems_close);
                distance_priority.add(2, systems_core);
                break;
            }
            default: {
                //random distance if the field wasn't properly filled
                distance_priority.add(0, systems_core);
                distance_priority.add(1, systems_close);
                distance_priority.add(2, systems_far);
                Collections.shuffle(distance_priority);
            }
        }

        //make sure the target entities list has something to look for
        List<String> desiredEntities = new ArrayList<>();
        if (entities == null || entities.isEmpty()) {
            desiredEntities.add(Tags.STABLE_LOCATION);
            desiredEntities.add(Tags.PLANET);
            desiredEntities.add(Tags.JUMP_POINT);
        } else {
            desiredEntities = entities;
        }

        //lets check the system lists in order by prefered distances
        for (int i = 0; i < 3; i++) {
            //check if the system list has anything, and shuffle it to ensure proper randomization
            if (distance_priority.get(i).isEmpty()) {
                continue;
            } else {
                Collections.shuffle(distance_priority.get(i));
            }

            //now check if any valid system got the required entity in this range band
            //starting with unexplored systems if required
            if (prioritizeUnexplored) {
                for (StarSystemAPI s : distance_priority.get(i)) {
                    //skip visited systems
                    if (s.isEnteredByPlayer()) {
                        continue;
                    }
                    //add all valid entities to the picker
                    WeightedRandomPicker<SectorEntityToken> validEntities = new WeightedRandomPicker<>();
                    for (SectorEntityToken e : s.getAllEntities()) {
                        for (String t : desiredEntities) {
                            if (e.hasTag(t)) {
                                validEntities.add(e);
                            }
                        }
                    }
                    //check it this system contains any target entity
                    if (!validEntities.isEmpty()) {
                        if (verbose) {
                            SectorEntityToken picked = validEntities.pick();
                            log.info("Selecting " + picked.getName() + ", in the " + picked.getContainingLocation().getName() + " system, " + picked.getContainingLocation().getLocation().length() + " (" + Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) + " LY) from the sector's center");
                            return picked;
                        } else return validEntities.pick();
                    }
                    //otherwise, the loop continues
                }

                //unexplored systems failed to offer the required entities, lets try the explored ones now
                for (StarSystemAPI s : distance_priority.get(i)) {
                    //skip unexplored systems this time
                    if (!s.isEnteredByPlayer()) {
                        continue;
                    }
                    //add all valid entities to the picker
                    WeightedRandomPicker<SectorEntityToken> validEntities = new WeightedRandomPicker<>();
                    for (SectorEntityToken e : s.getAllEntities()) {
                        for (String t : desiredEntities) {
                            if (e.hasTag(t)) {
                                validEntities.add(e);
                            }
                        }
                    }
                    //check it this system contains any target entity
                    if (!validEntities.isEmpty()) {
                        if (verbose) {
                            SectorEntityToken picked = validEntities.pick();
                            log.info("Selecting " + picked.getName() + ", in the " + picked.getContainingLocation().getName() + " system, " + picked.getContainingLocation().getLocation().length() + " (" + Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) + " LY) from the sector's center");
                            return picked;
                        } else return validEntities.pick();
                    }
                    //otherwise, the loop continues
                }
            } else {
                //unexplored systems isn't required, lets to ALL systems         
                for (StarSystemAPI s : distance_priority.get(i)) {
                    //add all valid entities to the picker
                    WeightedRandomPicker<SectorEntityToken> validEntities = new WeightedRandomPicker<>();
                    for (SectorEntityToken e : s.getAllEntities()) {
                        for (String t : desiredEntities) {
                            if (e.hasTag(t)) {
                                validEntities.add(e);
                            }
                        }
                    }
                    //check it this system contains any target entity
                    if (!validEntities.isEmpty()) {
                        if (verbose) {
                            SectorEntityToken picked = validEntities.pick();
                            log.info("Selecting " + picked.getName() + ", in the " + picked.getContainingLocation().getName() + " system, " + picked.getContainingLocation().getLocation().length() + " (" + Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) + " LY) from the sector's center");
                            return picked;
                        } else return validEntities.pick();
                    }
                    //otherwise, the loop continues
                }
            }

            //we exhausted all valid systems in the desired range band for the desired entities, then we can fallback to any entities
            if (defaultToAnyEntity) {
                WeightedRandomPicker<StarSystemAPI> randomSystemFallback = new WeightedRandomPicker<>();
                if (prioritizeUnexplored) {
                    //Lets try for unexplored systems in the desired range band and add the required entity there
                    for (StarSystemAPI s : distance_priority.get(i)) {
                        if (s.isEnteredByPlayer()) {
                            randomSystemFallback.add(s);
                        }
                    }
                    //no unexplored systems? Let's include ones that have not been visited in a while
                    if (randomSystemFallback.isEmpty()) {
                        for (StarSystemAPI s : distance_priority.get(i)) {
                            if (s.getDaysSinceLastPlayerVisit() > 365) {
                                randomSystemFallback.add(s);
                            }
                        }
                    }
                }
                //if there is not unexplored priority or if somehow every system in the required range band has been visited within the year... Somehow...
                if (randomSystemFallback.isEmpty()) {
                    for (StarSystemAPI s : distance_priority.get(i)) {
                        randomSystemFallback.add(s);
                    }
                }
                //alright, let's pick one system for our target
                StarSystemAPI selectedSystem = randomSystemFallback.pick();
                //and pick any entity

                if (verbose) {
                    SectorEntityToken picked = selectedSystem.getAllEntities().get(MathUtils.getRandomNumberInRange(0, selectedSystem.getAllEntities().size() - 1));
                    log.info("Selecting " + picked.getName() + ", in the " + picked.getContainingLocation().getName() + " system, " + picked.getContainingLocation().getLocation().length() + " (" + Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) + " LY) from the sector's center");
                    return picked;
                } else
                    return selectedSystem.getAllEntities().get(MathUtils.getRandomNumberInRange(0, selectedSystem.getAllEntities().size() - 1));

            }
            //and if that wasn't enough to find one single suitable system, use the next range band
        }
        //apparently none of the systems had any suitable target for the given filters, looks like this is a fail
        if (verbose) {
            log.warn("No valid system found");
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    //DUMPSTER
    ////////////////////////////////////////////////////////////////////////////

}
