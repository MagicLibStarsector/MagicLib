package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
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
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;

import java.util.*;

import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import static data.scripts.util.MagicTxt.nullStringIfEmpty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class MagicCampaign {
    
    public static Logger log = Global.getLogger(MagicCampaign.class);
    
    /**
     * Removes hyperspace clouds around the system, up to the outer-most jump point radius
     * 
     * @param system 
     * StarSystemAPI that needs cleanup
     */
    public static void hyperspaceCleanup(StarSystemAPI system){
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
	NebulaEditor editor = new NebulaEditor(plugin);        
        float minRadius = plugin.getTileSize() * 2f;
        
        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);	     
    }
    
    /**
     * Place an object on a stable orbit similar to the most approaching existing one that can be found 
     * @param object
     * @param spin 
     */
    public void placeOnStableOrbit(SectorEntityToken object, boolean spin){
        
        StarSystemAPI system = object.getStarSystem();
        Vector2f location = object.getLocation();
        
        //find a reference for the orbit
        SectorEntityToken referenceObject=null;
        float closestOrbit = 999999999;
        //find nearest orbit to match
        for(SectorEntityToken e : system.getAllEntities()){
            
            //skip non orpiting objects
            if(e.getOrbit()==null || e.getOrbitFocus()==null || e.getCircularOrbitRadius()<=0 || e.getCircularOrbitPeriod()<=0)continue;

            //skip stars
            if(e.isStar())continue;

            //find closest point on orbit for the tested object
            Vector2f closestPoint = MathUtils.getPoint(
                    e.getOrbitFocus().getLocation(),
                    e.getCircularOrbitRadius(),
                    VectorUtils.getAngle(e.getOrbitFocus().getLocation(), location)
            );

            //closest orbit becomes the reference
            if(MathUtils.getDistanceSquared(closestPoint, location)<closestOrbit){
                referenceObject=e;
            }
        }

        SectorEntityToken orbitCenter;
        Float angle,radius,period;
        
        if(referenceObject!=null){
            orbitCenter=referenceObject.getOrbitFocus();
            angle=VectorUtils.getAngle(referenceObject.getOrbitFocus().getLocation(),location);
            radius=MathUtils.getDistance(referenceObject.getOrbitFocus().getLocation(),location);
            period=referenceObject.getCircularOrbitPeriod()*(MathUtils.getDistance(referenceObject.getOrbitFocus().getLocation(),location)/referenceObject.getCircularOrbitRadius());                   
        } else {
            orbitCenter=system.getCenter();
            angle=VectorUtils.getAngle(system.getCenter().getLocation(),location);
            radius=MathUtils.getDistance(system.getCenter(),location);
            period=MathUtils.getDistance(system.getCenter(),location)/2;
        }
        
        object.setCircularOrbitWithSpin(orbitCenter,angle,period,radius,-10,10);
    }
    
    /**
     * Creates a derelict ship at the desired emplacement
     * 
     * @param variantId
     * spawned ship variant
     * @param condition
     * condition of the derelict,                                               better conditions means less D-mods but also more weapons from the variant
     * @param discoverable
     * awards XP when found
     * @param discoveryXp 
     * XP awarded when found (<0 to use the default)
     * @param recoverable
     * can be salvaged
     * @param orbitCenter
     * entity orbited
     * @param orbitStartAngle
     * orbit starting angle
     * @param orbitRadius
     * orbit radius
     * @param orbitDays
     * orbit period
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
    ){
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), false);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(orbitCenter.getStarSystem(), Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(discoverable);
        if(discoveryXp!=null && discoveryXp>=0){
            ship.setDiscoveryXP((float)discoveryXp);
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
     * @param id
     * field ID
     * @param radius
     * field radius in su (clamped to 1000)
     * @param density
     * field visual density
     * @param duration
     * field duration in days (set to a negative value for a permanent field)
     * @param glowDuration
     * time in days with glowing debris
     * @param salvageXp
     * XP awarded for salvaging (<0 to use the default)
     * @param defenderProbability
     * chance of an enemy fleet guarding the debris field (<0 to ignore)
     * @param defenderFaction
     * defender's faction
     * @param defenderFP
     * defender fleet's size in Fleet Points
     * @param detectionMult
     * detection distance multiplier (<0 to use the default)
     * @param discoverable
     * awards XP when found
     * @param discoveryXp 
     * XP awarded when found (<0 to use the default)
     * @param orbitCenter
     * entity orbited
     * @param orbitStartAngle
     * orbit starting angle
     * @param orbitRadius
     * orbit radius
     * @param orbitDays
     * orbit period
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
    ){

        float theDuration;
        if(duration<=0){
            theDuration=999999;
        } else {
            theDuration = duration;
        }        
        float theGlowDuration;
        if(glowDuration<0){
            theGlowDuration=0;
        } else {
            theGlowDuration = glowDuration;
        }
        
        DebrisFieldTerrainPlugin.DebrisFieldParams params = new DebrisFieldTerrainPlugin.DebrisFieldParams(
				Math.min(radius,1000), // field radius - should not go above 1000 for performance reasons
				density, // density, visual - affects number of debris pieces
				theDuration, // duration in days 
				theGlowDuration); // days the field will keep generating glowing pieces
        params.source = DebrisFieldSource.MIXED;     
        params.baseDensity=density;     
        if(salvageXp!=null && salvageXp>=0){
            params.baseSalvageXP = salvageXp; // base XP for scavenging in field
        }
        if(defenderProbability>0 && defenderFaction!=null && defenderFP!=null && defenderFP>0){
            params.defFaction=defenderFaction;
            params.defenderProb=defenderProbability;
            params.maxDefenderSize=defenderFP;
        }   
        SectorEntityToken generatedDebris = Misc.addDebrisField(orbitCenter.getStarSystem(), params, StarSystemGenerator.random);
        generatedDebris.setId(id);
        if(detectionMult>0){
            generatedDebris.setSensorProfile(detectionMult);
        }
        generatedDebris.setDiscoverable(discoverable);
        if(discoveryXp!=null && discoveryXp>=0){
            generatedDebris.setDiscoveryXP((float)discoveryXp);
        }
        generatedDebris.setCircularOrbit(orbitCenter, orbitStartAngle, orbitRadius, orbitDays);
        
        return generatedDebris;
    }
    
    public static enum lootType{
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
     * @param cargo
     * cargo to add salvage to, creates a new cargo if NULL
     * 
     * @param carrier
     * entity with the loot
     * 
     * @param type
     * MagicSystem.lootType, type of loot added,  
     * 
     * @param lootID
     * specific ID of the loot found, NULL for supplies, fuel, crew, marines,   trade commodities can use campaign.ids.Commodities
     * 
     * @param amount
     * 
     * @return 
     * The resulting cargo you can still add to. 
     */
    public static CargoAPI addSalvage(@Nullable CargoAPI cargo, SectorEntityToken carrier, lootType type, @Nullable String lootID, int amount){
        CargoAPI theCargo = Global.getFactory().createCargo(true);
        if(cargo!=null){
            theCargo = cargo;
        }
        switch(type){
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
     * Shorthand to add custom jump-point
     * 
     * @param id
     * jump point's internal ID
     * @param name
     * jump point's displayed name
     * @param linkedPlanet
     * planet displayed from hyperspace, can be null
     * @param orbitCenter
     * entity orbited
     * @param orbitStartAngle
     * orbit starting angle
     * @param orbitRadius
     * orbit radius
     * @param orbitDays
     * orbit period
     * @return 
     */
    public static SectorEntityToken createJumpPoint(
            String id,
            String name,
            @Nullable SectorEntityToken linkedPlanet,
            SectorEntityToken orbitCenter,
            float orbitStartAngle,
            float orbitRadius,
            float orbitDays
    ){
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(id, name);
        if(linkedPlanet!=null){
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
     * @param conditions
     * list of conditions from campaign.ids.Conditions
     * @param industries
     * list of industries from campaign.ids.Industries
     * @param hasStorage
     * @param paidForStorage
     * is storage already paid for
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
            List <String> conditions,
            List <String> industries,            
            boolean hasStorage,
            boolean paidForStorage,
            boolean hasBlackmarket,
            boolean hasOpenmarket,
            boolean hasMilitarymarket,
            boolean isAbandonedStation
    ){
        
        MarketAPI market = Global.getFactory().createMarket(id, name, size);
        market.setPrimaryEntity(entity);
        market.setFactionId(faction);
        market.setFreePort(isFreeport);
        market.setHidden(isHidden);
        
        //add conditions and industries
        if(!conditions.isEmpty()){
            for(String c : conditions){
                market.addCondition(c);
            }
        }
        if(!industries.isEmpty()){
            for(String i : industries){
                market.addIndustry(i);
            }
        }
        
        //add submarkets
        if(hasStorage){
            market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
            if(paidForStorage){
                ((StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()).setPlayerPaidToUnlock(true);
            }
        }
        if(hasBlackmarket){
            market.addSubmarket(Submarkets.SUBMARKET_BLACK);
        }
        if(hasOpenmarket){
            market.addSubmarket(Submarkets.SUBMARKET_OPEN);
        }
        if(hasMilitarymarket){
            market.addSubmarket(Submarkets.GENERIC_MILITARY);
        }
        
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        if(isAbandonedStation){
            entity.getMemoryWithoutUpdate().set("$abandonedStation", true);
        }
        entity.setMarket(market);  
        
        return market;
    }
    
    /**
     * Adds a custom character to a given market
     * 
     * @param market
     * MarketAPI the person is added to
     * @param firstName
     * person's first name
     * @param lastName
     * person's last name
     * @param portraitId
     * id of the sprite in settings.json/graphics/characters
     * @param gender
     * FullName.Gender
     * @param factionId
     * person's faction
     * @param rankId
     * rank from campaign.ids.Ranks
     * @param postId
     * post from campaign.ids.Ranks
     * @param isMarketAdmin
     * @param industrialPlanning_level
     * skill level for market admin
     * @param spaceOperation_level
     * skill level for market admin
     * @param groundOperations_level
     * skill level for market admin
     * @param commScreenPosition
     * position order in the comm screen, 0 is the admin position
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
            Integer spaceOperation_level,
            Integer groundOperations_level,
            Integer commScreenPosition
    ){
        
        PersonAPI person = Global.getFactory().createPerson();
        person.getName().setFirst(firstName);
        person.getName().setLast(lastName);
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", portraitId));
        person.setGender(gender);
        person.setFaction(factionId);
        
        person.setRankId(rankId);
        person.setPostId(postId);
        
        person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, industrialPlanning_level);
        person.getStats().setSkillLevel(Skills.SPACE_OPERATIONS, spaceOperation_level);
        person.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, groundOperations_level);

        if(isMarketAdmin){
            market.setAdmin(person);
        }
        market.getCommDirectory().addPerson(person, commScreenPosition);
        market.addPerson(person);
            
        return person;
    }


    /**
     * Creates a fleet with a defined flagship and optional escort
     * 
     * @param fleetName
     * @param fleetFaction
     * @param fleetType
     * campaign.ids.FleetTypes, default to FleetTypes.PERSON_BOUNTY_FLEET
     * @param flagshipName
     * Optional flagship name
     * @param flagshipVariant
     * @param captain
     * PersonAPI, can be NULL for random captain, otherwise use createCaptain() 
     * @param supportFleet
     * map <variantId, number> Optional escort ship VARIANTS and their NUMBERS
     * @param minFP
     * Minimal fleet size, can be used to adjust to the player's power,         set to 0 to ignore
     * @param reinforcementFaction
     * Reinforcement faction,                                                   if the fleet faction is a "neutral" faction without ships
     * @param qualityOverride
     * Optional ship quality override, default to 2 (no D-mods) if null or <0
     * @param spawnLocation
     * Where the fleet will spawn, default to assignmentTarget if NULL
     * @param assignment
     * campaign.FleetAssignment, default to orbit aggressive
     * @param assignementTarget
     * where the fleet will go to execute its order, it will not spawn if NULL
     * @param isImportant
     * @param transponderOn
     * @return 
     */
    public static CampaignFleetAPI createFleet(
            String fleetName,
            String fleetFaction,
            @Nullable String fleetType,
            @Nullable String flagshipName,
            String flagshipVariant,
            @Nullable PersonAPI captain,
            @Nullable Map<String, Integer> supportFleet,
            int minFP,
            String reinforcementFaction,
            @Nullable Float qualityOverride,
            @Nullable SectorEntityToken spawnLocation,
            @Nullable FleetAssignment assignment,
            @Nullable SectorEntityToken assignementTarget,
            boolean isImportant,
            boolean transponderOn
    ) {
        CampaignFleetAPI result = createFleet(fleetName, fleetFaction, fleetType, flagshipName, flagshipVariant,
                captain, supportFleet, minFP, reinforcementFaction, qualityOverride,
                spawnLocation, assignment, assignementTarget, isImportant, transponderOn, null);
        return result;
    }
    
    /**
     * Creates a fleet with a defined flagship and optional escort
     * 
     * @param fleetName
     * @param fleetFaction
     * @param fleetType
     * campaign.ids.FleetTypes, default to FleetTypes.PERSON_BOUNTY_FLEET
     * @param flagshipName
     * Optional flagship name
     * @param flagshipVariant
     * @param captain
     * PersonAPI, can be NULL for random captain, otherwise use createCaptain() 
     * @param supportFleet
     * map <variantId, number> Optional escort ship VARIANTS and their NUMBERS
     * @param minFP
     * Minimal fleet size, can be used to adjust to the player's power,         set to 0 to ignore
     * @param reinforcementFaction
     * Reinforcement faction,                                                   if the fleet faction is a "neutral" faction without ships
     * @param qualityOverride
     * Optional ship quality override, default to 2 (no D-mods) if null or <0
     * @param spawnLocation
     * Where the fleet will spawn, default to assignmentTarget if NULL
     * @param assignment
     * campaign.FleetAssignment, default to orbit aggressive
     * @param assignementTarget
     * where the fleet will go to execute its order, it will not spawn if NULL
     * @param isImportant
     * @param transponderOn
     * @param variantsPath
     * If not null, the script will try to find missing variant files there. 
     * Used to generate fleets using cross-mod variants that won't be loaded otherwise to avoid crashes.
     * The name of the variant files must match the ID of the variant.
     * @return 
     */
    public static CampaignFleetAPI createFleet(
            String fleetName,
            String fleetFaction,
            @Nullable String fleetType,
            @Nullable String flagshipName,
            String flagshipVariant,
            @Nullable PersonAPI captain,
            @Nullable Map<String, Integer> supportFleet,
            int minFP,
            String reinforcementFaction,
            @Nullable Float qualityOverride,
            @Nullable SectorEntityToken spawnLocation,
            @Nullable FleetAssignment assignment,
            @Nullable SectorEntityToken assignementTarget,
            boolean isImportant,
            boolean transponderOn,
            @Nullable String variantsPath
    ){
        //cleanup previous generation
        MagicVariables.presetShipIdsOfLastCreatedFleet.clear();
        boolean verbose = Global.getSettings().isDevMode();

        if(verbose){
            log.error(" ");
            log.error("SPAWNING " + fleetName);
            log.error(" ");
        }
        
        //Setup defaults
        String type = FleetTypes.PERSON_BOUNTY_FLEET;
        if(fleetType!=null && !fleetType.equals("")){
            type=fleetType;
        } else if(verbose){
            log.error("No fleet type defined, defaulting to bounty fleet.");
        }
        
        String extraShipsFaction = fleetFaction;
        if(reinforcementFaction!=null){
            extraShipsFaction=reinforcementFaction;
        } else if(verbose){
            log.error("No reinforcement faction defined, defaulting to fleet faction.");
        }
        
        SectorEntityToken location = assignementTarget;
        if(spawnLocation!=null){
            location=spawnLocation;
        } else if(verbose){
            log.error("No spawn location defined, defaulting to assignment target.");
        }
        
        FleetAssignment order = FleetAssignment.ORBIT_AGGRESSIVE;
        if(assignment!=null){
            order=assignment;
        } else if(verbose){
            log.error("No assignment defined, defaulting to aggressive orbit.");
        }
        
        Float quality = 2f;
        if(qualityOverride!=null && qualityOverride>=0){
            quality=qualityOverride;
        } else if(verbose){
            log.error("No quality override defined, defaulting to highest quality.");
        }
        
        //EMPTY FLEET
        CampaignFleetAPI bountyFleet = FleetFactoryV3.createEmptyFleet(fleetFaction, type, null);
        
        //ADDING FLAGSHIP
        FleetMemberAPI flagship = generateShip(flagshipVariant, variantsPath, true, verbose);
        if (flagship==null){
            log.error("Aborting "+fleetName+" generation");
            return null;
        }
        bountyFleet.getFleetData().addFleetMember(flagship);
        flagship.setFlagship(true);
        MagicVariables.presetShipIdsOfLastCreatedFleet.add(flagship.getId());
        
        //ADDING PRESET SHIPS IF REQUIRED
        if(supportFleet!=null && !supportFleet.isEmpty()){
            List<FleetMemberAPI> support = generatePresetShips(supportFleet, variantsPath, verbose);
            for (FleetMemberAPI m : support){
                bountyFleet.getFleetData().addFleetMember(m);
                MagicVariables.presetShipIdsOfLastCreatedFleet.add(m.getId());
            }
        }
        
        int coreFP = bountyFleet.getFleetPoints();
        
        //ADDING PROCGEN SHIPS IF REQUIRED
        if(minFP>0){
            if(verbose){
                if(minFP<coreFP){
                    log.warn("Preset FP: "+coreFP+", requested FP: "+minFP+". No reinforcements required.");
                } else {
                    log.warn("Preset FP: "+coreFP+", requested FP: "+minFP+". Adding "+(minFP-coreFP)+" FP worth of "+Global.getSector().getFaction(extraShipsFaction).getDisplayName()+" reinforcements.");
                }
            }
            
            if(minFP>coreFP){
                CampaignFleetAPI reinforcements = generateRandomFleet(extraShipsFaction, quality, type, (minFP-coreFP), 0.2f );
                
                List<FleetMemberAPI> membersInPriorityOrder = reinforcements.getFleetData().getMembersInPriorityOrder();
                if (membersInPriorityOrder!=null && !membersInPriorityOrder.isEmpty()){
                    for (FleetMemberAPI m : membersInPriorityOrder) {
                        m.setCaptain(null);
                        bountyFleet.getFleetData().addFleetMember(m);
                        //MagicVariables.presetShipIdsOfLastCreatedFleet.add(m.getId());
                    }
                }
            }
        }
        
        //ensuring the flagship is properly set
        bountyFleet.getFleetData().setFlagship(flagship);
        
        //ADDING OFFICERS
        FleetParamsV3 fleetParams = new FleetParamsV3(
                null,
                new Vector2f(),
                fleetFaction,
                quality,
                type,
                bountyFleet.getFleetPoints(),
                0f, 0f, 0f, 0f, 0f, 0f
        );
        FleetFactoryV3.addCommanderAndOfficersV2(bountyFleet, fleetParams, new Random());
        
        //ensuring the flagship is properly set AGAIN!
        bountyFleet.getFleetData().setFlagship(flagship);
        
        //I swear those sneaky officers are messing up the flagship tags
        if(verbose){
            log.warn("Fleet flagship is "+bountyFleet.getFlagship().getHullId());
            for(FleetMemberAPI m : bountyFleet.getMembersWithFightersCopy()){
                if(m.isFlagship()){
                    log.warn(m.getHullId()+" has the Flagship tag");
                }
            }
        } 
        for (FleetMemberAPI m : bountyFleet.getMembersWithFightersCopy()) {
            if (m==flagship){
                if(!m.isFlagship()){
                    m.setFlagship(true);
                    if(verbose){
                        log.warn("Adding flagship tag to "+m.getHullId());
                    }
                }
            } else if(m.isFlagship()){
                m.setFlagship(false);
                if(verbose){
                    log.warn("Removing flagship tag from "+m.getHullId());
                }
            }
        }    
        
        //add the defined captain to the flagship if needed
        if(captain!=null){
            bountyFleet.getFlagship().setCaptain(captain);
            bountyFleet.setCommander(flagship.getCaptain());
            if(verbose){
                log.warn("Assigning "+captain.getNameString()+" to the Flagship");
            }
        } else {
            bountyFleet.getFlagship().setCaptain(bountyFleet.getCommander());
            if(verbose){
                log.warn("Moving random commander to the Flagship");
            }
        }
        
        //apply skills to the fleet
        FleetFactoryV3.addCommanderSkills(bountyFleet.getCommander(), bountyFleet, fleetParams, new Random());
        
        
        //FINISHING
        
        bountyFleet.getFleetData().sort();
        bountyFleet.getFleetData().setSyncNeeded();
        bountyFleet.getFleetData().syncIfNeeded();
        
//        //debug
//        log.warn(bountyFleet.getMembersWithFightersCopy());
//        log.warn(bountyFleet.getFleetData().getMembersListWithFightersCopy().toString());
//        log.warn(bountyFleet.getFleetData().getMembersInPriorityOrder().toString());
        
        //cleanup name and faction
        bountyFleet.setNoFactionInName(true);
        bountyFleet.setFaction(fleetFaction, true);
        bountyFleet.setName(fleetName);

        //set standard 70% CR
        List<FleetMemberAPI> members = bountyFleet.getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : members) {
            member.getRepairTracker().setCR(0.7f);
        }
        
        //SPAWN if needed
        if (location != null) {
            spawnFleet(
                bountyFleet,
                location,
                order,
                assignementTarget,
                isImportant,
                transponderOn,
                verbose
            );
        }
        
        if(verbose){
            log.warn(fleetName+" creation completed");
        }
        
        return bountyFleet;
    }
    
    /**
     * Creates a captain PersonAPI
     * 
     * @param isAI
     * @param AICoreType
     * AI core from campaign.ids.Commodities
     * @param firstName
     * @param lastName
     * @param portraitId
     * id of the sprite in settings.json/graphics/characters
     * @param gender, any is gender-neutral, null is random male/female to avoid oddities and issues with dialogs and random portraits
     * @param factionId
     * @param rankId
     * rank from campaign.ids.Ranks
     * @param postId
     * post from campaign.ids.Ranks
     * @param personality
     * personality from campaign.ids.Personalities
     * @param level
     * Captain level, pick random skills according to the faction's doctrine
     * @param eliteSkillsOverride
     * Overrides the regular number of elite skills, set to -1 to ignore.
     * @param skillPreference
     * GENERIC, PHASE, CARRIER, ANY from OfficerManagerEvent.SkillPickPreference
     * @param skillLevels
     * Map <skill, level> Optional skills from campaign.ids.Skills and their appropriate levels, OVERRIDES ALL RANDOM SKILLS PREVIOUSLY PICKED
     * @return 
     */    
    public static PersonAPI createCaptain(
            boolean isAI,
            @Nullable String AICoreType,
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable String portraitId,
            @Nullable FullName.Gender gender,
            @NotNull String factionId,
            @Nullable String rankId,
            @Nullable String postId,
            @Nullable String personality,
            Integer level,
            Integer eliteSkillsOverride,
            @Nullable OfficerManagerEvent.SkillPickPreference skillPreference,
            @Nullable Map<String, Integer> skillLevels
    ){
        
        boolean verbose = Global.getSettings().isDevMode();
        
        PersonAPI person = OfficerManagerEvent.createOfficer(
                Global.getSector().getFaction(factionId),
                level,
                skillPreference,
                false, 
                null,
                true,
                eliteSkillsOverride!=0,
                eliteSkillsOverride,
                Misc.random
        );
        
        //try to create a default character of the proper gender if needed
        if(gender!=null && gender!=FullName.Gender.ANY && person.getGender()!=gender){
            for(int i=0;i<10;i++){
                person = OfficerManagerEvent.createOfficer(
                        Global.getSector().getFaction(factionId),
                        level,
                        skillPreference,
                        false, 
                        null,
                        true,
                        true,
                        eliteSkillsOverride,
                        Misc.random
                );
                if(person.getGender()==gender)break;
            }
        }
        
        if(gender!=null && gender!=FullName.Gender.ANY){
            person.setGender(FullName.Gender.ANY);
        }

        if(isAI){
            person.setAICoreId(AICoreType);  
            person.setGender(FullName.Gender.ANY);
        }
        
        if(nullStringIfEmpty(firstName)!=null){
            person.getName().setFirst(firstName);
        }
        
        if(nullStringIfEmpty(lastName)!=null){
            person.getName().setLast(lastName);
        }
        
        if(verbose){
            log.error(" ");
            log.error(" Creating captain " + person.getNameString());
            log.error(" ");
        }
        
        if (nullStringIfEmpty(portraitId) != null){
            if(portraitId.startsWith("graphics")){
                person.setPortraitSprite(portraitId);
            } else {
                person.setPortraitSprite(Global.getSettings().getSpriteName("characters", portraitId));
            }
        }
//        person.setFaction(factionId);
        if(nullStringIfEmpty(personality)!=null){
            person.setPersonality(personality);
        }
        if(verbose){
            log.error("     They are " + person.getPersonalityAPI().getDisplayName());
        }
        
        if(nullStringIfEmpty(rankId)!=null){
            person.setRankId(rankId);
        } else {
            person.setRankId(Ranks.CITIZEN);
        }
        
        if(nullStringIfEmpty(postId)!=null){
            person.setPostId(postId);
        } else {
            person.setPostId(Ranks.POST_SPACER);
        }
        
        //reset and reatribute skills if needed
        if(skillLevels!=null && !skillLevels.keySet().isEmpty()){
            if(verbose){
                for (SkillLevelAPI skill : person.getStats().getSkillsCopy()){
                    if(skillLevels.keySet().contains(skill.getSkill().getId())){
                        person.getStats().setSkillLevel(skill.getSkill().getId(),skillLevels.get(skill.getSkill().getId()));
                        log.error("     "+ skill.getSkill().getName() +" : "+ skillLevels.get(skill.getSkill().getId()));
                    } else {
                        person.getStats().setSkillLevel(skill.getSkill().getId(),0);                        
                        log.error("     "+ skill.getSkill().getName() +" : 0");
                    }
                }
            } else {
                for (SkillLevelAPI skill : person.getStats().getSkillsCopy()){
                    if(skillLevels.keySet().contains(skill.getSkill().getId())){
                        person.getStats().setSkillLevel(skill.getSkill().getId(),skillLevels.get(skill.getSkill().getId()));
                    } else {
                        person.getStats().setSkillLevel(skill.getSkill().getId(),0);
                    }
                }
            }
            person.getStats().refreshCharacterStatsEffects();
        } else if(verbose){
            // list assigned random skills
            log.error("     "+"level: "+ person.getStats().getLevel());
            for(MutableCharacterStatsAPI.SkillLevelAPI skill : person.getStats().getSkillsCopy()){
                if(skill.getSkill().isAptitudeEffect())continue;
                log.error("     "+" - "+ skill.getSkill().getName() +": "+ skill.getLevel());
            }
        }
        
        return person;
    }
    
    /**
     * Creates a ship variant from a regular variant file.
     * Used to create variants that requires different mods to be loaded.
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
            } catch (JSONException ignored) {}
            String hullId = obj.getString("hullId");
            JSONArray hullMods = obj.getJSONArray("hullMods");
            JSONArray modules = null;
            try {
                modules = obj.getJSONArray("modules");
            } catch (JSONException ignored) {}
            JSONArray permaMods = obj.getJSONArray("permaMods");
            JSONArray sMods = null;
            try {
                sMods = obj.getJSONArray("sMods");
            } catch (JSONException ignored) {}
            //float quality = (float) obj.getDouble("quality"); not used/available in API
            String variantId = obj.getString("variantId");
            JSONArray weaponGroups = obj.getJSONArray("weaponGroups");
            JSONArray wings = null;
            try {
                wings = obj.getJSONArray("wings");
            } catch (JSONException ignored) {}

            variant = Global.getSettings().createEmptyVariant(variantId, Global.getSettings().getHullSpec(hullId));
            variant.setVariantDisplayName(displayName);
            variant.setNumFluxCapacitors(fluxCapacitors);
            variant.setNumFluxVents(fluxVents);
            variant.setGoalVariant(goalVariant);
            // todo: check if order matters
            for (int i = 0; i < hullMods.length(); i++) {
                String hullModId = hullMods.getString(i);
                variant.addMod(hullModId);
            }
            for (int j = 0; j < permaMods.length(); j++) {
                String permaModId = hullMods.getString(j);
                variant.addPermaMod(permaModId);
            }
            if (sMods != null) {
                for (int k = 0; k < sMods.length(); k++) {
                    String sModId = hullMods.getString(k);
                    variant.addPermaMod(sModId, true);
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
            log.info("could not load ship variant at " + path, e);
        }
        return variant;
    }
    
    private static FleetMemberAPI generateShip(String variant, @Nullable String variantsPath, boolean flagship, boolean verbose) {        
        ShipVariantAPI thisVariant = Global.getSettings().getVariant(variant);
        //if the variant doesn't exist but a custom variant path is defined, try loading it
        if (thisVariant == null && variantsPath!=null) {
            thisVariant = loadVariant(variantsPath+variant+".variant");
        }
        if(thisVariant==null){
            return null;
        }        
        FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, thisVariant);  
        
        if (ship!=null) {
            ship.setFlagship(flagship);
            if(verbose) log.warn("Created "+variant);            
            return ship;
        }        
        
        log.error("Failed to create "+variant);
        return null;
    }

    private static List<FleetMemberAPI> generatePresetShips(Map<String, Integer> supportFleet, @Nullable String variantsPath, boolean verbose) {
        List<FleetMemberAPI> fleetMemberList = new ArrayList<>();
        for (String shipVariantId : supportFleet.keySet()) {
            for(int i=0; i<supportFleet.get(shipVariantId); i++){
                FleetMemberAPI fleetMember = generateShip(shipVariantId, variantsPath, false, verbose);
                if(fleetMember!=null) fleetMemberList.add(fleetMember);
            }
        }
        return fleetMemberList;
    }
    
    private static CampaignFleetAPI generateRandomFleet(String factionId, float qualityOverride, String fleetType, float fleetPoints, float freightersAndTankersFraction ) {
        
        FleetParamsV3 params = new FleetParamsV3(
                fakeMarket(factionId, qualityOverride),
                new Vector2f(),
                factionId,
                qualityOverride,
                fleetType,
                fleetPoints*(1-freightersAndTankersFraction),
                fleetPoints*(freightersAndTankersFraction/3),
                fleetPoints*(freightersAndTankersFraction/3), 
                fleetPoints*(freightersAndTankersFraction/3),
                0f, 0f,
                qualityOverride
        );
        
        params.ignoreMarketFleetSizeMult = true;
        params.maxNumShips = 50;
        params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;

        CampaignFleetAPI tempFleet = FleetFactoryV3.createFleet(params);
        if (tempFleet==null || tempFleet.isEmpty()) {
            log.warn("Failed to create procedural Support-Fleet");
            return null;
        }
        
        return tempFleet;
    }
    
    private static MarketAPI fakeMarket(String factionId, float quality_override){
        // create fake market and set ship quality
        MarketAPI market = Global.getFactory().createMarket("fake", "fake", 5);
        market.getStability().modifyFlat("fake", 10000);
        market.setFactionId(factionId);
        SectorEntityToken token = Global.getSector().getHyperspace().createToken(0, 0);
        market.setPrimaryEntity(token);
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("fake", quality_override);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("fake", 1f);
        
        return market;
    }
    
    /**
     * Spawn a fleet in its intended location with the proper order and target
     * @param fleet
     * @nullable @param spawnLocation
     * @nullable @param assignment
     * @param target
     * @param isImportant
     * @param transponderOn
     * @param verbose
     */
    public static void spawnFleet(
            CampaignFleetAPI fleet,
            @Nullable SectorEntityToken spawnLocation,
            @Nullable FleetAssignment assignment,
            SectorEntityToken target,
            boolean isImportant,
            boolean transponderOn,
            boolean verbose
            ){
        
        //defaults
        FleetAssignment order = FleetAssignment.ORBIT_AGGRESSIVE;
        if(assignment!=null){
            order=assignment;
        }        
        SectorEntityToken location = target;
        if(spawnLocation!=null){
            location=spawnLocation;
        }
        
        //spawn placement and assignement
        LocationAPI systemLocation = location.getContainingLocation();
        systemLocation.addEntity(fleet);
        fleet.setLocation(location.getLocation().x, location.getLocation().y);
        fleet.getAI().addAssignment(order, target, 1000000f, null);
        
        //radius fix
        fleet.forceSync();
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        
        //ancillary stuff
        fleet.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, isImportant);        
        fleet.setTransponderOn(transponderOn);
        
        if(verbose){
            log.warn("Spawned "+fleet.getName()+" around "+location.getId()+" in the "+location.getStarSystem().getId()+" system.");
            log.warn("Order: "+order.name()+", target: "+target.getId()+" in the "+target.getStarSystem().getId()+" system.");
        }
    }
    
    
    
    //BOUNTY CHECKS THAT MAY PROVE USEFULL FOR OTHER THINGS:    
    
    
    /**
     * 
     * @param market
     * checked market for triggers
     * @param market_id
     * list of preferred market IDs, bypass all other checks 
     * @param marketFaction_any
     * list of suitable faction IDs, any will do
     * @param marketFaction_alliedWith
     * market is suitable if least FAVORABLE with ANY of the required factions
     * @param marketFaction_none
     * list of unsuitable faction IDs
     * @param marketFaction_enemyWith
     * market is unsuitable if it is not HOSTILE with EVERY blacklisted factions
     * @param market_minSize
     * minimal market size
     * @return 
     */
    public static boolean isAvailableAtMarket(
            MarketAPI market,
            @Nullable List <String> market_id,
            @Nullable List <String> marketFaction_any,            
            boolean marketFaction_alliedWith,
            @Nullable List <String> marketFaction_none,           
            boolean marketFaction_enemyWith,
            int market_minSize
            ){

        List<String> marketBlacklist = MagicSettings.getList("MagicLib", "bounty_market_blacklist");

        if (marketBlacklist.contains(market.getId())) {
            return false;
        }
        
        //exact id match beats everything
        if(market_id!=null && !market_id.isEmpty()){
            for (String m : market_id){
                if (market.getId().equals(m))return true;
            }
        }
        
        //checking trigger_market_minSize
        if(market_minSize>0 && market.getSize()<market_minSize)return false;
                
        //checking trigger_marketFaction_none and trigger_marketFaction_enemyWith
        if(marketFaction_none!=null && !marketFaction_none.isEmpty()){
            for(String f : marketFaction_none){
                //skip non existing factions
                if(Global.getSector().getFaction(f)==null) {
                    log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    continue;
                }
                
                FactionAPI this_faction = Global.getSector().getFaction(f);
                if(market.getFaction()==this_faction){
                    return false; //is one of the excluded factions
                } else if(marketFaction_enemyWith && market.getFaction().isAtBest(this_faction, RepLevel.HOSTILE)){
                    return true; //is hostile with one of the excluded factions
                }
            }
        }
        
        //checking trigger_marketFaction_any and trigger_marketFaction_alliedWith
        if(marketFaction_any!=null && !marketFaction_any.isEmpty()){
            
            for(String f : marketFaction_any){
                //skip non existing factions
                if(Global.getSector().getFaction(f)==null) {
                    log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    continue;
                }
                
                FactionAPI this_faction = Global.getSector().getFaction(f);
                if(market.getFaction()==this_faction){
                    return true; //is one of the required factions
                } else if(marketFaction_alliedWith && market.getFaction().isAtWorst(this_faction, RepLevel.FAVORABLE)){
                    return true;  //is friendly toward one of the required factions
                }
            }
            return false; //the loop has not exited earlier, therefore it failed all of the market faction checks
        }
        
        //failed none of the tests, must be good then
        return true;
    }
    
    /**
     * 
     * @param player_minLevel
     * @param min_days_elapsed
     * @param min_fleet_size
     * @param memKeys_all
     * @param memKeys_any
     * @param playerRelationship_atLeast
     * @param playerRelationship_atMost
     * @return 
     */
    public static boolean isAvailableToPlayer(
            int player_minLevel,
            int min_days_elapsed,
            int min_fleet_size,
            @Nullable Map <String,Boolean> memKeys_all,          
            @Nullable Map <String,Boolean> memKeys_any,
            @Nullable Map <String,Float> playerRelationship_atLeast,  
            @Nullable Map <String,Float> playerRelationship_atMost
    ){
        
        //checking trigger_min_days_elapsed
        if(min_days_elapsed>0 && Global.getSector().getClock().getElapsedDaysSince(0)<min_days_elapsed)return false;
        
        //checking trigger_player_minLevel
        if(player_minLevel>0 && Global.getSector().getPlayerStats().getLevel()<player_minLevel)return false;
        
        //checking trigger_min_fleet_size
        if(min_fleet_size>0){
            CampaignFleetAPI playerFleet=Global.getSector().getPlayerFleet(); 
            float effectiveFP = playerFleet.getEffectiveStrength();
            return min_fleet_size < effectiveFP;
        }
        
        //checking trigger_playerRelationship_atLeast
        if(playerRelationship_atLeast!=null && !playerRelationship_atLeast.isEmpty()){
            for(String f : playerRelationship_atLeast.keySet()){
                //skip non existing factions
                if(Global.getSector().getFaction(f)==null) {
                    log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    continue;
                }
                if(!Global.getSector().getPlayerFaction().isAtWorst(f, RepLevel.getLevelFor(playerRelationship_atLeast.get(f))))return false;
            }
        }

        //checking trigger_playerRelationship_atMost
        if(playerRelationship_atMost!=null && !playerRelationship_atMost.isEmpty()){
            for(String f : playerRelationship_atMost.keySet()){
                //skip non existing factions
                if(Global.getSector().getFaction(f)==null) {
                    log.warn(String.format("Unable to find faction %s.", f), new RuntimeException());
                    continue;
                }
                if(!Global.getSector().getPlayerFaction().isAtBest(f, RepLevel.getLevelFor(playerRelationship_atMost.get(f))))return false;
            }
        }
        
        //checking trigger_memKeys_all
        if(memKeys_all!=null && !memKeys_all.isEmpty()){
            for(String f : memKeys_all.keySet()){
                //check if the memKey exists 
                if(!Global.getSector().getMemoryWithoutUpdate().getKeys().contains(f))return false;
                //check if it has the proper value
                if(memKeys_all.get(f)!=Global.getSector().getMemoryWithoutUpdate().getBoolean(f))return false;
            }
        }
        
        //checking trigger_memKeys_any
        if(memKeys_any!=null && !memKeys_any.isEmpty()){
            for(String f : memKeys_any.keySet()){
                //check if the memKey exists 
                if(!Global.getSector().getMemoryWithoutUpdate().getKeys().contains(f)){
                    //check if it has the proper value
                    if(memKeys_any.get(f)==Global.getSector().getMemoryWithoutUpdate().getBoolean(f))return true;
                }
            }
            //the loop has not been exited therefore some key is missing
            return false;
        }
        
        //failed none of the tests, must be good then
        return true;
    }
    
    public static Float PlayerThreatMultiplier(float enemyBaseFP){ //base FP is the min FP of the enemy fleet with reinforcements.
        CampaignFleetAPI playerFleet=Global.getSector().getPlayerFleet(); 
        float effectiveFP = playerFleet.getEffectiveStrength();
        return effectiveFP / enemyBaseFP;
    }
    
    /**
     * Returns a random target SectorEntityToken given the following parameters:
     * @param marketIDs
     * List of IDs of preferred markets to target, supercedes all,              default to other parameters if none of those markets exist
     * @param marketFactions
     * List of faction to pick a market from, supercedes all but market ids,    default to other parameters if none of those markets exist
     * @param distance
     * "CORE", "CLOSE", "FAR", preferred range band for the target system if any
     * @param seek_themes
     * List of preferred system themes TAGS from campaign.ids.Tags,             plus "PROCGEN_NO_THEME" and "PROCGEN_NO_THEME_NO_PULSAR_NO_BLACKHOLE"
     * @param avoid_themes
     * List of blacklisted system themes
     * @param entities
     * List of preferred entity types from campaign.ids.Tags
     * @param defaultToAnyEntity
     * If none of the systems in the required range band has any of the required entities, will the script default to any entity within a system with the proper range and themes instead of looking into a different range band
     * @param prioritizeUnexplored
     * Will the script target unexplored systems first before falling back to ones that have been visited by the player
     * @param verbose
     * Log some debug messages
     * @return 
     */
    @Nullable
    public static SectorEntityToken findSuitableTarget(
            @Nullable List<String> marketIDs,
            @Nullable List<String> marketFactions,
            @Nullable String distance,
            @Nullable List<String> seek_themes,
            @Nullable List<String> avoid_themes,
            @Nullable List<String> entities,
            boolean defaultToAnyEntity,
            boolean prioritizeUnexplored,
            boolean verbose){
        
        if(verbose){
            log.error("Find Suitable Target log");
            log.error("Checking marketIDs");
        }
        //first priority, check if the preset location(s) exist(s)
        if(marketIDs!=null && !marketIDs.isEmpty()){
            //if there is just one location and it exist, lets use that.
            if(marketIDs.size()==1 && Global.getSector().getEntityById(marketIDs.get(0))!=null){
                return Global.getSector().getEntityById(marketIDs.get(0));
            }
            //if there are multiple possible location, pick a random one
            WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
            for(String loc : marketIDs){
                if(Global.getSector().getEntityById(loc)!=null) picker.add(Global.getSector().getEntityById(loc));
            }
            
            if(verbose){
                log.error("There are "+picker.getTotal()+" available market ids for pick");
            }
            
            if(!picker.isEmpty()){
                if(verbose){
                    SectorEntityToken picked = picker.pick();
                    log.error("Selecting "+picked.getName()+", in the "+picked.getContainingLocation().getName()+" system, "+ picked.getContainingLocation().getLocation().length()+ " ("+ Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) +" LY) from the sector's center");
                    return picked;
                } else return picker.pick();
            }
        }
        
            
        if(verbose){
            log.error("Checking market factions");
        }
        //second priority is faction markets
        if(marketFactions!=null && !marketFactions.isEmpty()){
            WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>();
            for(MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()){
                if(marketFactions.contains(m.getFaction().getId()) && !picker.getItems().contains(m.getPrimaryEntity())){
                    picker.add(m.getPrimaryEntity());
                }
            }
            
            if(verbose){
                log.error("There are "+picker.getTotal()+" available faction markets for pick");
            }
            
            if(!picker.isEmpty()){
                if(verbose){
                    SectorEntityToken picked = picker.pick();
                    log.error("Selecting "+picked.getName()+", in the "+picked.getContainingLocation().getName()+" system, "+ picked.getContainingLocation().getLocation().length()+ " ("+ Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) +" LY) from the sector's center");
                    return picked;
                } else return picker.pick();
            }
        }
        
        if(
                (distance==null||distance.equals("")) && 
                (seek_themes==null||seek_themes.isEmpty()) && 
                (entities==null||entities.isEmpty())
                ){
            //there was no fallback filters defined, this is a wrap
            return null;
        }
        
        //time for some pain
        
        //calculate the sector size to define range bands
//        final HyperspaceTerrainPlugin hyper = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
//        final int[][] cells = hyper.getTiles();
        float sector_width = MagicVariables.getSectorSize();
        
        if(verbose){
            log.error("Checking preferences");
            log.error("Sector width = "+sector_width);
        }
        if(verbose){
            log.error("Finding system with required themes");
        }
        //start with the themes preferences
        List <StarSystemAPI> systems_core = new ArrayList<>();
        List <StarSystemAPI> systems_close = new ArrayList<>();
        List <StarSystemAPI> systems_far = new ArrayList<>();
        if(seek_themes!=null && !seek_themes.isEmpty()){
            for(StarSystemAPI s : Global.getSector().getStarSystems()){
                for(String this_theme : seek_themes){
                    if(s.hasTag(this_theme)){
                        //sort systems by distances because that will come in handy later
                        float dist = s.getLocation().length();
                        if(dist<sector_width*0.33f){
                            systems_core.add(s);
                        } else if (dist<sector_width*0.66f){
                            systems_close.add(s);
                        } else {
                            systems_far.add(s);
                        }
                        break;
                    }
                }
                //special test for basic procgen systems without special content
                if(seek_themes.contains("procgen_no_theme") || seek_themes.contains("procgen_no_theme_pulsar_blackhole")){
                    if(s.isProcgen()){
                        if(seek_themes.contains("procgen_no_theme_pulsar_blackhole") && (s.hasBlackHole() || s.hasPulsar()))continue;
                        //check for the 3 bland themes
                        if(s.getTags().contains("theme_misc_skip") || s.getTags().contains("theme_misc") ||  s.getTags().contains("theme_core_unpopulated")){
                            //sort systems by distances because that will come in handy later
                            float dist = s.getLocation().length();
                            if(dist<sector_width*0.33f){
                                systems_core.add(s);
                            } else if (dist<sector_width*0.66f){
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
            for(StarSystemAPI s : Global.getSector().getStarSystems()){
                //sort systems by distances because that will come in handy later
                float dist = s.getLocation().length();
                if(dist<sector_width*0.33f){
                    systems_core.add(s);
                } else if (dist<sector_width*0.66f){
                    systems_close.add(s);
                } else {
                    systems_far.add(s);
                }
            }
        }
        
        //cull systems with blacklisted themes
        if(avoid_themes!=null && !avoid_themes.isEmpty()){
            
            if(!systems_core.isEmpty()){
                for(int i=0; i<systems_core.size(); i++){
                    for(String t : avoid_themes){
                        if(systems_core.get(i).getTags().contains(t)){
                            systems_core.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
            if(!systems_close.isEmpty()){
                for(int i=0; i<systems_close.size(); i++){
                    for(String t : avoid_themes){
                        if(systems_close.get(i).getTags().contains(t)){
                            systems_close.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
            if(!systems_far.isEmpty()){
                for(int i=0; i<systems_far.size(); i++){
                    for(String t : avoid_themes){
                        if(systems_far.get(i).getTags().contains(t)){
                            systems_far.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
        }
        
        if(verbose){
                log.error("There are "+systems_core.size()+" themed systems in the core");
                log.error("There are "+systems_close.size()+" themed systems close to the core");
                log.error("There are "+systems_far.size()+" themed systems far from the core");
        }
                        
        //TO DO: check if ALL lists are empty
        
        //now order the selected system lists by distance preferences
        List <List<StarSystemAPI>> distance_priority = new ArrayList<>();
        if(distance==null || distance.equals("")){
            //random distance 
            distance_priority.add(0, systems_core);
            distance_priority.add(1, systems_close);
            distance_priority.add(2, systems_far);
            Collections.shuffle(distance_priority);            
        } else switch (distance){
            case "CORE":{
                distance_priority.add(0, systems_core);
                distance_priority.add(1, systems_close);
                distance_priority.add(2, systems_far);
                break;
            }
            case "CLOSE":{
                distance_priority.add(0, systems_close);
                distance_priority.add(1, systems_far);
                distance_priority.add(2, systems_core);
                break;
            }
            case "FAR":{
                distance_priority.add(0, systems_far);
                distance_priority.add(1, systems_close);
                distance_priority.add(2, systems_core);
                break;
            }
            default :{
                //random distance if the field wasn't properly filled
                distance_priority.add(0, systems_core);
                distance_priority.add(1, systems_close);
                distance_priority.add(2, systems_far);
                Collections.shuffle(distance_priority);    
            }
        }
        
        //make sure the target entities list has something to look for
        List<String> desiredEntities = new ArrayList<>();
        if(entities==null || entities.isEmpty()){
            desiredEntities.add(Tags.STABLE_LOCATION);
            desiredEntities.add(Tags.PLANET);
            desiredEntities.add(Tags.JUMP_POINT);
        } else {
            desiredEntities=entities;
        }
        
        //lets check the system lists in order by prefered distances
        for(int i=0; i<3; i++){
            //check if the system list has anything, and shuffle it to ensure proper randomization
            if(distance_priority.get(i).isEmpty()){
                continue;
            } else {
                Collections.shuffle(distance_priority.get(i));
            }
            
            //now check if any valid system got the required entity in this range band
            //starting with unexplored systems if required
            if(prioritizeUnexplored){                
                for(StarSystemAPI s : distance_priority.get(i)){
                    //skip visited systems
                    if(s.isEnteredByPlayer()){
                        continue;
                    }
                    //add all valid entities to the picker
                    WeightedRandomPicker<SectorEntityToken> validEntities = new WeightedRandomPicker<>();
                    for(SectorEntityToken e : s.getAllEntities()){
                        for(String t : desiredEntities){
                            if(e.hasTag(t)){
                                validEntities.add(e);
                            }
                        }
                    }
                    //check it this system contains any target entity
                    if(!validEntities.isEmpty()){
                        if(verbose){
                            SectorEntityToken picked = validEntities.pick();
                            log.error("Selecting "+picked.getName()+", in the "+picked.getContainingLocation().getName()+" system, "+ picked.getContainingLocation().getLocation().length()+ " ("+ Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) +" LY) from the sector's center");
                            return picked;
                        } else return validEntities.pick();
                    }
                    //otherwise, the loop continues
                }
                
                //unexplored systems failed to offer the required entities, lets try the explored ones now
                for(StarSystemAPI s : distance_priority.get(i)){
                    //skip unexplored systems this time
                    if(!s.isEnteredByPlayer()){
                        continue;
                    }
                    //add all valid entities to the picker
                    WeightedRandomPicker<SectorEntityToken> validEntities = new WeightedRandomPicker<>();
                    for(SectorEntityToken e : s.getAllEntities()){
                        for(String t : desiredEntities){
                            if(e.hasTag(t)){
                                validEntities.add(e);
                            }
                        }
                    }
                    //check it this system contains any target entity
                    if(!validEntities.isEmpty()){
                        if(verbose){
                            SectorEntityToken picked = validEntities.pick();
                            log.error("Selecting "+picked.getName()+", in the "+picked.getContainingLocation().getName()+" system, "+ picked.getContainingLocation().getLocation().length()+ " ("+ Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) +" LY) from the sector's center");
                            return picked;
                        } else return validEntities.pick();
                    }
                    //otherwise, the loop continues
                }
            } else {
                //unexplored systems isn't required, lets to ALL systems         
                for(StarSystemAPI s : distance_priority.get(i)){
                    //add all valid entities to the picker
                    WeightedRandomPicker<SectorEntityToken> validEntities = new WeightedRandomPicker<>();
                    for(SectorEntityToken e : s.getAllEntities()){
                        for(String t : desiredEntities){
                            if(e.hasTag(t)){
                                validEntities.add(e);
                            }
                        }
                    }
                    //check it this system contains any target entity
                    if(!validEntities.isEmpty()){
                        if(verbose){
                            SectorEntityToken picked = validEntities.pick();
                            log.error("Selecting "+picked.getName()+", in the "+picked.getContainingLocation().getName()+" system, "+ picked.getContainingLocation().getLocation().length()+ " ("+ Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) +" LY) from the sector's center");
                            return picked;
                        } else return validEntities.pick();
                    }
                    //otherwise, the loop continues
                }
            }
                        
            //we exhausted all valid systems in the desired range band for the desired entities, then we can fallback to any entities
            if(defaultToAnyEntity){
                WeightedRandomPicker<StarSystemAPI> randomSystemFallback = new WeightedRandomPicker<>();
                if(prioritizeUnexplored){
                    //Lets try for unexplored systems in the desired range band and add the required entity there
                    for(StarSystemAPI s : distance_priority.get(i)){
                        if(s.isEnteredByPlayer()){
                            randomSystemFallback.add(s);
                        }
                    }
                    //no unexplored systems? Let's include ones that have not been visited in a while
                    if(randomSystemFallback.isEmpty()){
                        for(StarSystemAPI s : distance_priority.get(i)){
                            if(s.getDaysSinceLastPlayerVisit()>365){
                                randomSystemFallback.add(s);
                            }
                        }
                    }
                }
                //if there is not unexplored priority or if somehow every system in the required range band has been visited within the year... Somehow...
                if(randomSystemFallback.isEmpty()){
                    for(StarSystemAPI s : distance_priority.get(i)){
                        randomSystemFallback.add(s);
                    }
                }
                //alright, let's pick one system for our target
                StarSystemAPI selectedSystem = randomSystemFallback.pick();
                //and pick any entity

                if(verbose){
                    SectorEntityToken picked = selectedSystem.getAllEntities().get(MathUtils.getRandomNumberInRange(0, selectedSystem.getAllEntities().size()-1));
                    log.error("Selecting "+picked.getName()+", in the "+picked.getContainingLocation().getName()+" system, "+ picked.getContainingLocation().getLocation().length()+ " ("+ Misc.getDistanceLY(new Vector2f(), picked.getContainingLocation().getLocation()) +" LY) from the sector's center");
                    return picked;
                } else return selectedSystem.getAllEntities().get(MathUtils.getRandomNumberInRange(0, selectedSystem.getAllEntities().size()-1));
                
            }
            //and if that wasn't enough to find one single suitable system, use the next range band
        }
        //apparently none of the systems had any suitable target for the given filters, looks like this is a fail
        if(verbose){
            log.error("No valid system found");
        }
        return null;
    }
    
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    //DUMPSTER
    ////////////////////////////////////////////////////////////////////////////
    
    
    
    
    
//    private static FleetMemberAPI FLAGSHIP=null;
    
    /**
     * Creates a fleet with a defined flagship and optional escort
     * 
     * @param fleetName
     * @param fleetFaction
     * @param fleetType
     * campaign.ids.FleetTypes, default to FleetTypes.PERSON_BOUNTY_FLEET
     * @param flagshipName
     * Optional flagship name
     * @param flagshipVariant
     * @param captain
     * PersonAPI, can be NULL for random captain, otherwise use createCaptain() 
     * @param supportFleet
     * map <variantId, number> Optional escort ship VARIANTS and their NUMBERS
     * @param minFP
     * Minimal fleet size, can be used to adjust to the player's power,         set to 0 to ignore
     * @param reinforcementFaction
     * Reinforcement faction,                                                   if the fleet faction is a "neutral" faction without ships
     * @param qualityOverride
     * Optional ship quality override, default to 2 (no D-mods) if null or <0
     * @param spawnLocation
     * Where the fleet will spawn, default to assignmentTarget if NULL
     * @param assignment
     * campaign.FleetAssignment, default to orbit aggressive
     * @param assignementTarget
     * @param isImportant
     * @param transponderOn
     * @param variantsPath
     * If not null, the script will try to find missing variant files there. 
     * Used to generate fleets using cross-mod variants that won't be loaded otherwise to avoid crashes.
     * The name of the variant files must match the ID of the variant.
     * @return 
     */
//    public static CampaignFleetAPI createFleet(
//            String fleetName,
//            String fleetFaction,
//            @Nullable String fleetType,
//            @Nullable String flagshipName,
//            String flagshipVariant,
//            @Nullable PersonAPI captain,
//            @Nullable Map<String, Integer> supportFleet,
//            int minFP,
//            String reinforcementFaction,
//            @Nullable Float qualityOverride,
//            @Nullable SectorEntityToken spawnLocation,
//            @Nullable FleetAssignment assignment,
//            @Nullable SectorEntityToken assignementTarget,
//            boolean isImportant,
//            boolean transponderOn,
//            @Nullable String variantsPath
//    ) {
//        
//        
//        
//        
//        
//        
//        /*
//        //enforce clean generation
//        theSupportFleet.clear();
//        presetShipIdsOfLastCreatedFleet.clear();
//        theFlagshipVariant=null;
//        theFlagship=null;
//        theFleet=null;
//        */
//        MagicVariables.presetShipIdsOfLastCreatedFleet.clear();
//        boolean verbose = Global.getSettings().isDevMode();
//
//        if(verbose){
//            log.error(" ");
//            log.error("SPAWNING " + fleetName);
//            log.error(" ");
//        }
//    
//        String type = FleetTypes.PERSON_BOUNTY_FLEET;
//        if(fleetType!=null && !fleetType.equals("")){
//            type=fleetType;
//        } else if(verbose){
//            log.error("No fleet type defined, defaulting to bounty fleet.");
//        }
//        
//        String extraShipsFaction = fleetFaction;
//        if(reinforcementFaction!=null){
//            extraShipsFaction=reinforcementFaction;
//        } else if(verbose){
//            log.error("No reinforcement faction defined, defaulting to fleet faction.");
//        }
//        
//        SectorEntityToken location = assignementTarget;
//        if(spawnLocation!=null){
//            location=spawnLocation;
//        } else if(verbose){
//            log.error("No spawn location defined, defaulting to assignment target.");
//        }
//        
//        FleetAssignment order = FleetAssignment.ORBIT_AGGRESSIVE;
//        if(assignment!=null){
//            order=assignment;
//        } else if(verbose){
//            log.error("No assignment defined, defaulting to aggressive orbit.");
//        }
//        
//        Float quality = 2f;
//        if(qualityOverride!=null && qualityOverride>=0){
//            quality=qualityOverride;
//        } else if(verbose){
//            log.error("No quality override defined, defaulting to highest quality.");
//        }
//        
//        ////////////////////////////
//        //NON RANDOM FLEET ELEMENT//
//        ////////////////////////////       
//        
//        /*
//        theFlagshipVariant=flagshipVariant;
//        if(supportFleet!=null && !supportFleet.isEmpty()){
//            theSupportFleet=supportFleet;
//        } 
//        */
//        
//        FleetParamsV3 params = new FleetParamsV3(
//                null,
//                assignementTarget != null ? assignementTarget.getLocationInHyperspace() : null,
//                fleetFaction,
//                //2f, // qualityOverride
//                qualityOverride,
//                type,
//                0f, 0f, 0f, 0f, 0f, 0f, 0f
//        );
//        params.ignoreMarketFleetSizeMult = true;
//
//        //create a fleet using the defined flagship variant and escort ships if any
//        CampaignFleetAPI newFleet = generateFleet(params, flagshipVariant, supportFleet, variantsPath); // nonrandom fleet, flagship and preset variants
//        if (newFleet == null || newFleet.isEmpty()) {
//            if(verbose){
//                log.error("Fleet spawned empty, possibly due to missing flagship - aborting");
//            }
//            return null;
//        }
////        FleetMemberAPI flagship = newFleet.getFlagship();
//
//        ////////////////////////////
//        // RANDOM SHIPS ADDITIONS //
//        ////////////////////////////  
//        
//        //calculate missing portion of the fleet
//        int currPts = newFleet.getFleetPoints();
//        int extraPts = 0;        
//        if(verbose){
//            log.warn("pregenerated fleet is " + currPts + " FP, minimum fleet FP is " + minFP);
//            if (currPts < minFP) {
//                extraPts = minFP - currPts;
//                log.warn("adding " + extraPts + " extra FP of random ships to hit minimum");
//            }
//        } else {
//            if (currPts < minFP) {
//                extraPts = minFP - currPts;
//            }
//        }
//        
//        //tweak the existing fleet generation to add random ships
//        params.combatPts = extraPts;
//        params.doNotPrune = true;
//        params.factionId=extraShipsFaction;
//        params.quality = quality;
//        params.qualityOverride = quality;
//        CampaignFleetAPI extraFleet = FleetFactoryV3.createFleet(params);
//
//        //only add the proper amount of ships, starting by the larger ones
//        /*
//        List<FleetMemberAPI> holding = new ArrayList<>();
//        for (FleetMemberAPI mem : extraFleet.getFleetData().getMembersInPriorityOrder()) {
//            holding.add(mem);
//        }
//        for (FleetMemberAPI held : holding) {
//            extraFleet.getFleetData().removeFleetMember(held);
//            newFleet.getFleetData().addFleetMember(held);
//        }
//        */
//        
//        //making sure there is no flagship override there 
//        for (FleetMemberAPI mem : extraFleet.getFleetData().getMembersInPriorityOrder()) {
//            if(mem.isFlagship())mem.setFlagship(false);
//            newFleet.getFleetData().addFleetMember(mem);
//        }
//        
//        ///////////////////////////
//        //FINISH FLEET GENERATION//
//        ///////////////////////////
//        
//        
//        //add the defined captain to the flagship, apply skills to the fleet
//        if(captain!=null){
//            newFleet.getFlagship().setCaptain(captain);
//        }
//        
//        if(flagshipName!=null){
//            newFleet.getFlagship().setShipName(flagshipName);
//        }
//        newFleet.setCommander(newFleet.getFlagship().getCaptain());
//        FleetFactoryV3.addCommanderSkills(newFleet.getCommander(), newFleet, Misc.random);
//
//        //cleanup name and faction
//        newFleet.setNoFactionInName(true);
//        newFleet.setFaction(fleetFaction, true);
//        newFleet.setName(fleetName);
//
//        //spawn placement and assignement
//        if (location != null) {
//            LocationAPI systemLocation = location.getContainingLocation();
//            systemLocation.addEntity(newFleet);
//            newFleet.setLocation(location.getLocation().x, location.getLocation().y);
//            newFleet.getAI().addAssignment(order, assignementTarget, 1000000f, null);
//        }
//
//        //set standard 70% CR
//        List<FleetMemberAPI> members = newFleet.getFleetData().getMembersListCopy();
//        for (FleetMemberAPI member : members) {
//            member.getRepairTracker().setCR(0.7f);
//        }
//        
//        //radius fix
//        newFleet.forceSync();
//        newFleet.getFleetData().setSyncNeeded();
//        newFleet.getFleetData().syncIfNeeded();
//        
//        newFleet.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, isImportant);        
//        newFleet.setTransponderOn(transponderOn);
//        /*
//        //Triple down on enforcing the proper flagship
//        for (FleetMemberAPI m : newFleet.getMembersWithFightersCopy()) {
//            if (m==FLAGSHIP){
//                m.setFlagship(true);
//            } else {
//                m.setFlagship(false);
//            }
//        }
//        FLAGSHIP=null;
//        */
//        return newFleet;
//    }
    
    
    /*
    private static CampaignFleetAPI generateFleet(
            FleetParamsV3 params,
            String flagshipVariant,
            @Nullable Map<String, Integer> supportFleet,
            @Nullable String variantsPath
    ) {

        boolean verbose = Global.getSettings().isDevMode();
        
        // create fake market and set ship quality
        MarketAPI market = Global.getFactory().createMarket("fake", "fake", 5);
        market.getStability().modifyFlat("fake", 10000);
        market.setFactionId(params.factionId);
        SectorEntityToken token = Global.getSector().getHyperspace().createToken(0, 0);
        market.setPrimaryEntity(token);
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("fake", BASE_QUALITY_WHEN_NO_MARKET);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("fake", 1f);
        params.source = market;

        // set faction
        String factionId = params.factionId;

        // create the fleet object
        CampaignFleetAPI newFleet = createEmptyFleet(factionId, params.fleetType, market);
        //theFleet = newFleet;
//        newFleet.getFleetData().setOnlySyncMemberLists(true);

        Random random = new Random();
        if (params.random != null) {
            random = params.random;
        }
        
        // add boss
        FleetMemberAPI flag = addToFleet(flagshipVariant, newFleet, random, variantsPath, true);
        if (flag == null) {
            if(verbose){
                log.error(flagshipVariant + " does not exist, aborting");
            }
            return null;
        }
        MagicVariables.presetShipIdsOfLastCreatedFleet.add(flag.getId());
        
        FLAGSHIP=flag;
        
        // add support
        if (supportFleet!=null && !supportFleet.isEmpty()) {
            for (String v : supportFleet.keySet()) {
                for(int i=0; i<supportFleet.get(v); i++){
                    FleetMemberAPI test = addToFleet(v, newFleet, random, variantsPath, false);
                    if (test == null && verbose) {
                        log.warn(v + " not found, skipping that variant");
                    } else if (test != null) {
                        MagicVariables.presetShipIdsOfLastCreatedFleet.add(test.getId());
                        //just to make sure
                        if(test.isFlagship())test.setFlagship(false);
                    }
                }
            }
        }
        
        //enforcing proper flagship just to be sure
//        if ( newFleet.getFlagship() != flag ) {
//            newFleet.getFlagship().setFlagship(false);
//            flag.setFlagship(true);
//        }
        
        if (params.withOfficers) {
            addCommanderAndOfficers(newFleet, params, random);
        }

        newFleet.forceSync();

        if (newFleet.getFleetData().getNumMembers() <= 0
                || newFleet.getFleetData().getNumMembers() == newFleet.getNumFighters()) {
        }
        params.source = null;

        newFleet.setInflater(null); // no autofit

        newFleet.getFleetData().setOnlySyncMemberLists(false);
        
        return newFleet;
    }
    */
    
    /*
    protected static FleetMemberAPI addToFleet(String variant, CampaignFleetAPI fleet, Random random, @Nullable String variantsPath, boolean flagship) {

        FleetMemberAPI member;
        ShipVariantAPI test = Global.getSettings().getVariant(variant);
        //if the variant doesn't exist but a custom variant path is defined, try loading it
        if (test == null && variantsPath!=null) {
            test = loadVariant(variantsPath+variant+".variant");
        }
        if(test==null){
            return null;
        }

        member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, test);
        String name = fleet.getFleetData().pickShipName(member, random);
        member.setShipName(name);
        member.setFlagship(flagship);
        fleet.getFleetData().addFleetMember(member);
        return member;
    }
    */
    
}
