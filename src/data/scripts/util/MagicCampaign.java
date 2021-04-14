package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.BASE_QUALITY_WHEN_NO_MARKET;
import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.addCommanderAndOfficers;
import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.createEmptyFleet;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

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
     * forces D-mods presence regardless of the condition 
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
            Integer salvageXp,
            float defenderProbability,
            @Nullable String defenderFaction,
            Integer defenderFP,
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
        if(salvageXp>=0){
            params.baseSalvageXP = salvageXp; // base XP for scavenging in field
        }
        if(defenderProbability>0 && defenderFaction!=null && defenderFP>0){
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
     * FullName.Gender value
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

    private static CampaignFleetAPI theFleet;
    private static FleetMemberAPI theFlagship;
    private static String theFlagshipVariant;
    private static Map<String, Integer> theSupportFleet = new HashMap<>();
    
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
     * @return 
     */
    public static SectorEntityToken createFleet(
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
            SectorEntityToken assignementTarget,
            boolean isImportant,
            boolean transponderOn
    ) {
        //enforce clean generation
        theSupportFleet.clear();
        theFlagshipVariant=null;
        theFlagship=null;
        theFleet=null;
        boolean verbose = Global.getSettings().isDevMode();

        if(verbose){
            log.error(" ");
            log.error("SPAWNING " + fleetName);
            log.error(" ");
        }
    
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
        
        //NON RANDOM FLEET ELEMENT
                
        theFlagshipVariant=flagshipVariant;
        if(supportFleet!=null && !supportFleet.isEmpty()){
            theSupportFleet=supportFleet;
        } 
        
        FleetParamsV3 params = new FleetParamsV3(
                null,
                assignementTarget.getLocationInHyperspace(),
                fleetFaction,
                2f, // qualityOverride
                type,
                0f, 0f, 0f, 0f, 0f, 0f, 0f
        );
        params.ignoreMarketFleetSizeMult = true;

        //create a fleet using the defined flagship variant and escort ships if any
        CampaignFleetAPI newFleet = createFleet(params); // nonrandom fleet, flagship and preset variants
        if (newFleet == null || newFleet.isEmpty()) {
            if(verbose){
                log.error("Fleet spawned empty, possibly due to missing flagship - aborting");
            }
            return null;
        }

        //if needed, complete the fleet using relevant random ships
        
        //calculate missing portion of the fleet
        int currPts = newFleet.getFleetPoints();
        int extraPts = 0;        
        if(verbose){
            log.info("pregenerated fleet is " + currPts + " FP, minimum fleet FP is " + minFP);
            if (currPts < minFP) {
                extraPts = minFP - currPts;
                log.info("adding " + extraPts + " extra FP of random ships to hit minimum");
            }
        } else {
            if (currPts < minFP) {
                extraPts = minFP - currPts;
            }
        }
        
        //tweak the existing fleet generation to add random ships
        params.combatPts = extraPts;
        params.doNotPrune = true;
        params.factionId=extraShipsFaction;
        params.quality = quality;
        params.qualityOverride = quality;
        CampaignFleetAPI extraFleet = FleetFactoryV3.createFleet(params);

        //only add the proper amount of ships, starting by the larger ones
        List<FleetMemberAPI> holding = new ArrayList<>();
        for (FleetMemberAPI mem : extraFleet.getFleetData().getMembersInPriorityOrder()) {
            holding.add(mem);
        }
        for (FleetMemberAPI held : holding) {
            extraFleet.getFleetData().removeFleetMember(held);
            newFleet.getFleetData().addFleetMember(held);
        }
        if (!newFleet.getFlagship().equals(theFlagship)) {
            newFleet.getFlagship().setFlagship(false);
            theFlagship.setFlagship(true);
        }

        //add the defined captain to the flagship, apply skills to the fleet
        if(captain!=null){
            theFlagship.setCaptain(captain);
        }
        if(flagshipName!=null){
            theFlagship.setShipName(flagshipName);
        }
        newFleet.setCommander(theFlagship.getCaptain());
        FleetFactoryV3.addCommanderSkills(theFleet.getCommander(), theFleet, null);

        //cleanup name and faction
        newFleet.setNoFactionInName(true);
        newFleet.setFaction(fleetFaction, true);
        newFleet.setName(fleetName);

        //spawn placement and assignement
        LocationAPI systemLocation = location.getContainingLocation();
        systemLocation.addEntity(newFleet);
        newFleet.setLocation(location.getLocation().x, location.getLocation().y);
        newFleet.getAI().addAssignment(order, assignementTarget, 1000000f, null);

        //set standard 70% CR
        List<FleetMemberAPI> members = newFleet.getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : members) {
            member.getRepairTracker().setCR(0.7f);
        }
        
        //radius fix
        newFleet.forceSync();
        newFleet.getFleetData().setSyncNeeded();
        newFleet.getFleetData().syncIfNeeded();
        
        newFleet.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, isImportant);        
        newFleet.setTransponderOn(transponderOn);
        
        return newFleet;
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
     * @param gender
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
            String firstName,
            String lastName,
            String portraitId,
            FullName.Gender gender,
            String factionId,
            String rankId,
            String postId,
            String personality,
            Integer level,
            Integer eliteSkillsOverride,
            @Nullable OfficerManagerEvent.SkillPickPreference skillPreference,
            @Nullable Map<String, Integer> skillLevels
    ){
        
        boolean verbose = Global.getSettings().isDevMode();
        
        if(verbose){
            log.error(" ");
            log.error(" Creating captain " + firstName + " " + lastName);
            log.error(" ");
        }
        
//        PersonAPI person = Global.getFactory().createPerson();
        PersonAPI person =
//                OfficerManagerEvent.createOfficer(Global.getSector().getFaction(factionId), level, false);
        OfficerManagerEvent.createOfficer(
                Global.getSector().getFaction(factionId),
                level,
                skillPreference,
                true, 
                null,
                true,
                true,
                eliteSkillsOverride,
                null
        );
        
        if(isAI){
            person.setAICoreId(AICoreType);  
            person.getName().setFirst(firstName); 
            person.getName().setLast(lastName);
            person.setGender(FullName.Gender.ANY);
        } else{
            person.getName().setFirst(firstName);
            person.getName().setLast(lastName);
            person.setGender(gender);
        }
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", portraitId));
        person.setFaction(factionId);
        person.setPersonality(personality);
        if(verbose){
            log.error("     They are " + personality);
        }
        
        person.setRankId(rankId);
        person.setPostId(postId);
        
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
        }
        
        return person;
    }
    
    /**
     * @deprecation THIS DECLARATION IS MISSING AN AI CORE TYPE FOR AI CAPTAINS
     * Creates a captain PersonAPI
     * 
     * @param isAI
     * @param firstName
     * @param lastName
     * @param portraitId
     * id of the sprite in settings.json/graphics/characters
     * @param gender
     * @param factionId
     * @param rankId
     * rank from campaign.ids.Ranks
     * @param postId
     * post from campaign.ids.Ranks
     * @param personality
     * personality from campaign.ids.Personalities
     * @param level
     * Captain level, pick random skills according to the faction's doctrine
     * @param skillLevels
     * Map <skill, level> Optional skills from campaign.ids.Skills and their appropriate levels, OVERRIDES ALL RANDOM SKILLS PREVIOUSLY PICKED
     * @return 
     */
    @Deprecated
    public static PersonAPI createCaptain(
            boolean isAI,
            String firstName,
            String lastName,
            String portraitId,
            FullName.Gender gender,
            String factionId,
            String rankId,
            String postId,
            String personality,
            Integer level,
            @Nullable Map<String, Integer> skillLevels
    ){
        log.error("DEPRECATION WARNING!");
        log.error("DEPRECATION WARNING!");
        log.error(" ");
        log.error("OBSOLETE DECLARATION OF MagicCampaign.createCaptain()");
        log.error(" ");
        log.error("DEPRECATION WARNING!");
        log.error("DEPRECATION WARNING!");
        
        boolean verbose = Global.getSettings().isDevMode();
        
        if(verbose){
            log.error(" ");
            log.error(" Creating captain " + firstName + " " + lastName);
            log.error(" ");
        }
        
//        PersonAPI person = Global.getFactory().createPerson();
        PersonAPI person = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(factionId), level, false);
        
        if(isAI){
            person.setAICoreId(Commodities.ALPHA_CORE);  
            person.getName().setFirst(firstName); 
            person.getName().setLast(lastName);
            person.setGender(FullName.Gender.ANY);
        } else{
            person.getName().setFirst(firstName);
            person.getName().setLast(lastName);
            person.setGender(gender);
        }
        person.setPortraitSprite(Global.getSettings().getSpriteName("characters", portraitId));
        person.setFaction(factionId);
        person.setPersonality(personality);
        if(verbose){
            log.error("     They are " + personality);
        }
        
        person.setRankId(rankId);
        person.setPostId(postId);
        
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
        }
        
        return person;
    }
    
    private static CampaignFleetAPI createFleet(FleetParamsV3 params) {

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
        theFleet = newFleet;
        newFleet.getFleetData().setOnlySyncMemberLists(true);

        Random random = new Random();
        if (params.random != null) {
            random = params.random;
        }
        
        // add boss
        FleetMemberAPI flag = addToFleet(theFlagshipVariant, newFleet, random);
        if (flag == null) {
            if(verbose){
                log.error(theFlagshipVariant + " does not exist, aborting");
            }
            return null;
        }
        theFlagship = flag;
        
        // add support
        if (theSupportFleet!=null && !theSupportFleet.isEmpty()) {
            for (String v : theSupportFleet.keySet()) {
                for(int i=0; i<theSupportFleet.get(v); i++){
                    FleetMemberAPI test = addToFleet(v, newFleet, random);
                    if (test == null && verbose) {
                        log.warn(v + " not found, skipping that variant");
                    }
                }
            }
        }

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
    
    protected static FleetMemberAPI addToFleet(String variant, CampaignFleetAPI fleet, Random random) {

        FleetMemberAPI member;
        ShipVariantAPI test = Global.getSettings().getVariant(variant);
        if (test == null) {
            return null;
        }

        member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, test);
        String name = fleet.getFleetData().pickShipName(member, random);
        member.setShipName(name);
        fleet.getFleetData().addFleetMember(member);
        return member;
    }
}