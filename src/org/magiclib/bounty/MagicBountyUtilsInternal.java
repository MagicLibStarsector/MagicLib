package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.CoreRuleTokenReplacementGeneratorImpl;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.magiclib.util.MagicTxt;
import org.magiclib.util.StringCreator;

import java.util.Map;

/**
 * Do not use this.
 * It is only public because it is used in the org.magiclib.bounty.rulecmd package.
 */
public class MagicBountyUtilsInternal {

    /**
     * Replaces variables in the given string with data from the bounty and splits it into paragraphs using `\n`.
     */
    public static String replaceStringVariables(final ActiveBounty bounty, String text) {
        String replaced = text;

        // Perform vanilla replacements.
        CoreRuleTokenReplacementGeneratorImpl coreRuleTokenReplacementGenerator = new CoreRuleTokenReplacementGeneratorImpl();
        Map<String, String> tokenReplacements = coreRuleTokenReplacementGenerator.getTokenReplacements(null, bounty.getFleet(), null);

        for (Map.Entry<String, String> replacement : tokenReplacements.entrySet()) {
            replaced = replaced.replace(replacement.getKey(), replacement.getValue());
        }

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$sonDaughterChild", () -> {
            return switch (bounty.getFleet().getCommander().getGender()) {
                case MALE -> MagicTxt.getString("mb_son");
                case FEMALE -> MagicTxt.getString("mb_daughter");
                default -> MagicTxt.getString("mb_child");
            };
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$fatherMotherParent", () -> {
            return switch (bounty.getFleet().getCommander().getGender()) {
                case MALE -> MagicTxt.getString("mb_father");
                case FEMALE -> MagicTxt.getString("mb_mother");
                default -> MagicTxt.getString("mb_parent");
            };
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$manWomanPerson", () -> {
            if (bounty.getFleet().getCommander().isAICore()) {
                return MagicTxt.getString("mb_ai");
            } else {
                return switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE -> MagicTxt.getString("mb_man");
                    case FEMALE -> MagicTxt.getString("mb_woman");
                    default -> MagicTxt.getString("mb_person");
                };
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$hisHerTheir", () -> {
            if (bounty.getFleet().getCommander().isAICore()) {
                return MagicTxt.getString("mb_its");
            } else {
                return switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE -> MagicTxt.getString("mb_his");
                    case FEMALE -> MagicTxt.getString("mb_her");
                    default -> MagicTxt.getString("mb_their");
                };
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$heSheThey", () -> {
            if (bounty.getFleet().getCommander().isAICore()) {
                return MagicTxt.getString("mb_it");
            } else {
                return switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE -> MagicTxt.getString("mb_he");
                    case FEMALE -> MagicTxt.getString("mb_she");
                    default -> MagicTxt.getString("mb_they");
                };
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$heIsSheIsTheyAre", () -> {
            if (bounty.getFleet().getCommander().isAICore()) {
                return MagicTxt.getString("mb_itIs");
            } else {
                return switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE -> MagicTxt.getString("mb_heIs");
                    case FEMALE -> MagicTxt.getString("mb_sheIs");
                    default -> MagicTxt.getString("mb_theyAre");
                };
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$himHerThem", () -> {
            if (bounty.getFleet().getCommander().isAICore()) {
                return MagicTxt.getString("mb_it");
            } else {
                return switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE -> MagicTxt.getString("mb_him");
                    case FEMALE -> MagicTxt.getString("mb_her");
                    default -> MagicTxt.getString("mb_them");
                };
            }
        });

        StringCreator reflexivePronounStringCreator = () -> {
            if (bounty.getFleet().getCommander().isAICore()) {
                return MagicTxt.getString("mb_itself");
            } else {
                return switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE -> MagicTxt.getString("mb_himself");
                    case FEMALE -> MagicTxt.getString("mb_herself");
                    default -> MagicTxt.getString("mb_themselves");
                };
            }
        };
        // Typo fixed in 0.46.0
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$himslefHerselfThemselves", reflexivePronounStringCreator);
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$himselfHerselfThemselves", reflexivePronounStringCreator);


        replaced = MagicTxt.replaceAllIfPresent(replaced, "$system", () -> bounty.getFleetSpawnLocation().getContainingLocation().getNameWithNoType());
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$shipName", () -> bounty.getFleet().getFlagship().getShipName());
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$location", () -> {
            if (bounty.getFleetSpawnLocation().getMarket() != null) {
                return bounty.getFleetSpawnLocation().getMarket().getName();
            }
            return bounty.getFleetSpawnLocation().getFullName();
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$givingFaction", () -> bounty.getGivingFaction() != null
                ? bounty.getGivingFaction().getDisplayNameWithArticle()
                : "a faction");
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$rewardFaction", () -> {
            FactionAPI rewardFaction = Global.getSector().getFaction(bounty.getRewardFactionId());
            return rewardFaction != null
                    ? rewardFaction.getDisplayNameWithArticle()
                    : "a faction";
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$targetFaction", () -> bounty.getFleet().getFaction().getDisplayNameWithArticle());
        // Deprecated in 1.1.2
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$faction", () -> bounty.getFleet().getFaction().getDisplayNameWithArticle());
        // The reward the bounty pays, after scaling and rounding. A bounty without a credit reward keeps job_credit_reward.
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$reward", () -> bounty.getRewardCredits() != null
                ? Misc.getDGSCredits(bounty.getRewardCredits())
                : Misc.getDGSCredits(bounty.getSpec().job_credit_reward));
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$name", () -> bounty.getFleet().getCommander().getNameString());
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$firstName", () -> bounty.getFleet().getCommander().getName().getFirst());
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$lastName", () -> bounty.getFleet().getCommander().getName().getLast());
        // The core worlds and hyperspace have no constellation. Like vanilla's location text, name the system instead.
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$constellation", () -> {
            LocationAPI location = bounty.getFleetSpawnLocation().getContainingLocation();
            return location.getConstellation() != null
                    ? location.getConstellation().getName()
                    : location.getNameWithNoType();
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$shipFleet", () -> {
            if (bounty.getFleet().getFleetData().getMembersListCopy().size() == 1) {
                return MagicTxt.getString("mb_var_ship");
            } else {
                return MagicTxt.getString("mb_var_fleet");
            }
        });

        //FAILSAFE
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$", () -> "==error==");

        return replaced;
    }

    public static String createLocationEstimateText(final ActiveBounty bounty) {
//        SectorEntityToken hideoutLocation = bounty.getFleetSpawnLocation();
//        SectorEntityToken fake = hideoutLocation.getContainingLocation().createToken(0, 0);
//        fake.setOrbit(Global.getFactory().createCircularOrbit(hideoutLocation, 0, 1000, 100));
//
//        String loc = BreadcrumbSpecial.getLocatedString(fake);

        String loc = BreadcrumbSpecial.getLocatedString(bounty.getFleetSpawnLocation());
//        loc = loc.replaceAll(getString("mb_distance_orbit"), getString("mb_distance_hidingNear"));
//        loc = loc.replaceAll(getString("mb_distance_located"), getString("mb_distance_hidingIn"));
        String sheIs = getPronoun(bounty.getCaptain());
        if (bounty.getCaptain().isAICore()) sheIs = MagicTxt.getString("mb_distance_it");
        loc = sheIs + MagicTxt.getString("mb_distance_rumor") + loc + MagicTxt.getString(".");

        return loc;
    }

    public static String getPronoun(@NotNull PersonAPI personAPI) {
        return switch (personAPI.getGender()) {
            case FEMALE -> MagicTxt.getString("mb_distance_she");
            case MALE -> MagicTxt.getString("mb_distance_he");
            default -> MagicTxt.getString("mb_distance_they");
        };
    }

    public static String createLocationPreciseText(final ActiveBounty bounty) {

        String loc = MagicTxt.getString("mb_distance_last");

        if (bounty.getSpec().fleet_behavior == FleetAssignment.PATROL_SYSTEM) {
            loc = loc + MagicTxt.getString("mb_distance_roaming") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
        } else {
            if (bounty.getFleetSpawnLocation().getMarket() != null) {
                loc = loc + MagicTxt.getString("mb_distance_near") + bounty.getFleetSpawnLocation().getMarket().getName() + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.PLANET)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + bounty.getFleetSpawnLocation().getName() + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.STATION)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_station") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.JUMP_POINT)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_jump") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.GATE)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_gate") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.DEBRIS_FIELD)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_debris") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.WRECK)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_wreck") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.COMM_RELAY)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_comm") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.SENSOR_ARRAY)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_sensor") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.NAV_BUOY)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_nav") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.STABLE_LOCATION)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_stable") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else {
                loc = loc + MagicTxt.getString("mb_distance_somewhere") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            }
        }
        loc = loc + MagicTxt.getString(".");
        return loc;
    }
}
